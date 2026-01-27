package ceui.lisa.jcstaff.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 加载任务状态
 */
enum class LoadTaskState {
    IDLE,       // 空闲，还未开始
    LOADING,    // 加载中
    PAUSED,     // 暂停（页面退出但任务未完成）
    COMPLETED,  // 完成
    FAILED      // 失败
}

/**
 * 加载任务
 * 包含进度信息和缓存文件路径，可被多个页面共享
 */
data class LoadTask(
    val taskId: String,                    // 任务唯一标识（通常是URL）
    val state: LoadTaskState = LoadTaskState.IDLE,
    val progress: Float = 0f,              // 进度 0-1
    val bytesLoaded: Long = 0L,            // 已加载字节数
    val totalBytes: Long = 0L,             // 总字节数
    val cachedFilePath: String? = null,    // 缓存文件路径（下载完成后可直接使用）
    val error: String? = null,             // 错误信息
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val activeListeners: Int = 0           // 活跃监听者数量
) {
    /**
     * 进度百分比 (0-100)
     */
    val progressPercent: Int
        get() = (progress * 100).toInt()

    /**
     * 是否正在加载
     */
    val isLoading: Boolean
        get() = state == LoadTaskState.LOADING

    /**
     * 是否已完成
     */
    val isCompleted: Boolean
        get() = state == LoadTaskState.COMPLETED

    /**
     * 是否失败
     */
    val isFailed: Boolean
        get() = state == LoadTaskState.FAILED

    /**
     * 是否有活跃的监听者
     */
    val hasActiveListeners: Boolean
        get() = activeListeners > 0

    /**
     * 是否需要继续加载（未完成且有监听者）
     */
    val shouldContinueLoading: Boolean
        get() = !isCompleted && !isFailed && hasActiveListeners

    /**
     * 是否有可用的缓存文件
     */
    val hasCachedFile: Boolean
        get() = cachedFilePath != null && File(cachedFilePath).exists()
}

/**
 * 应用级加载任务管理器
 *
 * 主要功能：
 * 1. 一级详情页的进度，二级详情页可以共享
 * 2. 从页面退出再进入，续上上一个请求而不是新发请求
 * 3. 自己维护 OkHttp 下载，下载完成后保存到缓存文件
 * 4. 点击下载按钮时直接使用缓存文件，瞬间完成
 *
 * 设计模式：
 * - 单例模式：全局唯一实例
 * - 观察者模式：通过 StateFlow 实现进度变更通知
 * - 引用计数：追踪活跃监听者，支持任务复用
 */
object LoadTaskManager {

    private val tasks = ConcurrentHashMap<String, MutableStateFlow<LoadTask>>()
    private val listenerCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var cacheDir: File? = null

    /**
     * 初始化（在 Application 中调用）
     */
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "image_load_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 初始化（用户隔离）
     */
    fun init(context: Context, userId: Long) {
        cacheDir = File(context.cacheDir, "image_load_cache_$userId").apply {
            if (!exists()) mkdirs()
        }
        // 清除内存中的任务状态
        resetInMemoryState()
    }

    /**
     * 重置内存中的任务状态
     */
    fun resetInMemoryState() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        tasks.clear()
        listenerCounts.clear()
    }

    /**
     * 获取缓存目录
     */
    private fun getCacheDir(): File {
        return cacheDir ?: throw IllegalStateException("LoadTaskManager not initialized. Call init(context) first.")
    }

    /**
     * 根据 URL 生成缓存文件名
     */
    private fun getCacheFile(url: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val extension = url.substringAfterLast('.', "jpg").substringBefore('?')
        return File(getCacheDir(), "$hash.$extension")
    }

    /**
     * 注册监听并启动下载（页面进入时调用）
     * 如果任务已存在且已完成，直接返回
     * 如果任务不存在或未完成，启动/继续下载
     */
    fun registerListener(taskId: String): StateFlow<LoadTask> {
        val count = listenerCounts.getOrPut(taskId) { AtomicInteger(0) }
        val newCount = count.incrementAndGet()

        val flow = tasks.getOrPut(taskId) {
            MutableStateFlow(LoadTask(taskId = taskId, activeListeners = 1))
        }

        // 更新监听者数量
        flow.update { it.copy(activeListeners = newCount, lastUpdatedAt = System.currentTimeMillis()) }

        val task = flow.value

        // 检查是否有缓存文件
        val cacheFile = getCacheFile(taskId)
        if (cacheFile.exists() && cacheFile.length() > 0) {
            // 有缓存，直接标记为完成
            flow.update {
                it.copy(
                    state = LoadTaskState.COMPLETED,
                    progress = 1f,
                    cachedFilePath = cacheFile.absolutePath,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
            return flow.asStateFlow()
        }

        // 如果任务未完成且没有正在进行的下载，启动下载
        if (!task.isCompleted && !downloadJobs.containsKey(taskId)) {
            startDownload(taskId)
        }

        return flow.asStateFlow()
    }

    /**
     * 启动下载任务
     */
    private fun startDownload(taskId: String) {
        val job = scope.launch {
            try {
                updateState(taskId) { it.copy(state = LoadTaskState.LOADING) }

                val cacheFile = getCacheFile(taskId)
                val tempFile = File(cacheFile.parent, "${cacheFile.name}.tmp")

                val request = Request.Builder()
                    .url(taskId)
                    .addHeader("Referer", "https://app-api.pixiv.net/")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()

                // 写入临时文件并追踪进度
                body.source().use { source ->
                    tempFile.sink().buffer().use { sink ->
                        var totalBytesRead = 0L
                        var bytesRead: Long

                        while (source.read(sink.buffer, 8192).also { bytesRead = it } != -1L) {
                            totalBytesRead += bytesRead
                            sink.emitCompleteSegments()

                            // 更新进度
                            updateProgress(taskId, totalBytesRead, contentLength)
                        }
                    }
                }

                // 下载完成，重命名为正式文件
                tempFile.renameTo(cacheFile)

                // 标记完成
                updateState(taskId) {
                    it.copy(
                        state = LoadTaskState.COMPLETED,
                        progress = 1f,
                        cachedFilePath = cacheFile.absolutePath,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                }

            } catch (e: Exception) {
                updateState(taskId) {
                    it.copy(
                        state = LoadTaskState.FAILED,
                        error = e.message,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                }
            } finally {
                downloadJobs.remove(taskId)
            }
        }

        downloadJobs[taskId] = job
    }

    /**
     * 取消监听（页面退出时调用）
     * 减少监听者计数，当计数为0时将任务标记为暂停（不取消下载）
     */
    fun unregisterListener(taskId: String) {
        val count = listenerCounts[taskId] ?: return
        val newCount = count.decrementAndGet().coerceAtLeast(0)

        tasks[taskId]?.update { task ->
            val newState = if (newCount == 0 && task.state == LoadTaskState.LOADING) {
                LoadTaskState.PAUSED
            } else {
                task.state
            }
            task.copy(
                activeListeners = newCount,
                state = newState,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * 更新任务状态
     */
    private fun updateState(taskId: String, transform: (LoadTask) -> LoadTask) {
        tasks[taskId]?.update(transform)
    }

    /**
     * 获取任务（不增加监听者计数）
     */
    fun getTask(taskId: String): StateFlow<LoadTask>? {
        return tasks[taskId]?.asStateFlow()
    }

    /**
     * 获取任务当前值
     */
    fun peekTask(taskId: String): LoadTask? {
        return tasks[taskId]?.value
    }

    /**
     * 获取缓存文件路径
     */
    fun getCachedFilePath(taskId: String): String? {
        val task = tasks[taskId]?.value
        if (task?.hasCachedFile == true) {
            return task.cachedFilePath
        }
        // 也检查磁盘上是否有缓存
        val cacheFile = getCacheFile(taskId)
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile.absolutePath
        }
        return null
    }

    /**
     * 更新进度
     */
    fun updateProgress(taskId: String, bytesLoaded: Long, totalBytes: Long) {
        val flow = tasks[taskId] ?: return

        val progress = if (totalBytes > 0) {
            (bytesLoaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        flow.update { task ->
            if (task.state == LoadTaskState.LOADING || task.state == LoadTaskState.PAUSED || task.state == LoadTaskState.IDLE) {
                task.copy(
                    state = LoadTaskState.LOADING,
                    progress = progress,
                    bytesLoaded = bytesLoaded,
                    totalBytes = totalBytes,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            } else {
                task
            }
        }
    }

    /**
     * 标记任务完成
     */
    fun markCompleted(taskId: String, cachedFilePath: String? = null) {
        tasks[taskId]?.update { task ->
            task.copy(
                state = LoadTaskState.COMPLETED,
                progress = 1f,
                cachedFilePath = cachedFilePath ?: task.cachedFilePath,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * 标记任务失败
     */
    fun markFailed(taskId: String, error: String? = null) {
        tasks[taskId]?.update { task ->
            task.copy(
                state = LoadTaskState.FAILED,
                error = error,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * 重试下载
     */
    fun retry(taskId: String) {
        // 取消现有任务
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)

        // 删除临时文件
        val cacheFile = getCacheFile(taskId)
        val tempFile = File(cacheFile.parent, "${cacheFile.name}.tmp")
        tempFile.delete()

        // 重置状态
        tasks[taskId]?.update {
            it.copy(
                state = LoadTaskState.IDLE,
                progress = 0f,
                bytesLoaded = 0L,
                totalBytes = 0L,
                cachedFilePath = null,
                error = null,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }

        // 重新开始下载
        startDownload(taskId)
    }

    /**
     * 检查任务是否存在且正在进行
     */
    fun hasActiveTask(taskId: String): Boolean {
        val task = tasks[taskId]?.value ?: return false
        return task.state == LoadTaskState.LOADING || task.state == LoadTaskState.PAUSED
    }

    /**
     * 检查任务是否已完成
     */
    fun isCompleted(taskId: String): Boolean {
        return tasks[taskId]?.value?.isCompleted == true
    }

    /**
     * 获取任务进度
     */
    fun getProgress(taskId: String): Float? {
        return tasks[taskId]?.value?.progress
    }

    /**
     * 移除任务
     */
    fun removeTask(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        tasks.remove(taskId)
        listenerCounts.remove(taskId)
    }

    /**
     * 清理已完成的任务（从内存中移除，不删除缓存文件）
     */
    fun cleanupCompletedTasks(olderThanMs: Long = 5 * 60 * 1000) {
        val now = System.currentTimeMillis()
        val toRemove = tasks.entries
            .filter { (_, flow) ->
                val task = flow.value
                task.isCompleted &&
                task.activeListeners == 0 &&
                (now - task.lastUpdatedAt) > olderThanMs
            }
            .map { it.key }

        toRemove.forEach { taskId ->
            tasks.remove(taskId)
            listenerCounts.remove(taskId)
        }
    }

    /**
     * 清理缓存文件
     */
    fun clearCache() {
        getCacheDir().listFiles()?.forEach { it.delete() }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return getCacheDir().listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * 获取所有任务数量
     */
    fun taskCount(): Int = tasks.size

    /**
     * 获取活跃任务数量
     */
    fun activeTaskCount(): Int = tasks.count { it.value.value.isLoading }
}

/**
 * 创建与 ProgressManager 集成的监听器
 */
fun createLoadTaskProgressListener(taskId: String): ProgressListener {
    return { _, bytesRead, contentLength ->
        LoadTaskManager.updateProgress(taskId, bytesRead, contentLength)
    }
}
