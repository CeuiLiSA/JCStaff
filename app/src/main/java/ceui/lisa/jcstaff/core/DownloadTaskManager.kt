package ceui.lisa.jcstaff.core

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import ceui.lisa.jcstaff.cache.AppDatabase
import ceui.lisa.jcstaff.cache.DownloadStatus
import ceui.lisa.jcstaff.cache.DownloadTaskDao
import ceui.lisa.jcstaff.cache.DownloadTaskEntity
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 下载任务管理器
 *
 * 负责：
 * - 管理下载任务队列
 * - 执行下载操作
 * - 追踪下载进度
 * - App 重启后恢复未完成的任务
 */
object DownloadTaskManager {

    private const val TAG = "DownloadTaskManager"
    private const val MAX_CONCURRENT_DOWNLOADS = 3

    private var dao: DownloadTaskDao? = null
    private var appContext: Context? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadMutex = Mutex()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentDownloadId = MutableStateFlow<Long?>(null)
    val currentDownloadId: StateFlow<Long?> = _currentDownloadId.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context, userId: Long) {
        appContext = context.applicationContext
        val db = AppDatabase.getInstanceForUser(context, userId)
        dao = db.downloadTaskDao()
        Log.d(TAG, "DownloadTaskManager initialized for user $userId")

        // 恢复中断的下载任务
        scope.launch {
            dao?.resetDownloadingToPending()
            if (!isInitialized) {
                isInitialized = true
                processQueue()
            }
        }
    }

    fun reset() {
        dao = null
        appContext = null
        isInitialized = false
        _isProcessing.value = false
        _currentDownloadId.value = null
        Log.d(TAG, "DownloadTaskManager reset")
    }

    // ==================== 任务管理 ====================

    /**
     * 添加下载任务
     */
    fun addTask(illust: Illust) {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                val pageCount = illust.page_count ?: illust.meta_pages?.size ?: 1
                val entity = DownloadTaskEntity(
                    illustId = illust.id,
                    title = illust.title ?: "",
                    previewUrl = illust.previewUrl(),
                    width = illust.width,
                    height = illust.height,
                    userId = illust.user?.id ?: 0L,
                    userName = illust.user?.name ?: "",
                    userAvatarUrl = illust.user?.profile_image_urls?.findAvatarUrl(),
                    illustJson = gson.toJson(illust),
                    status = DownloadStatus.PENDING,
                    totalPages = pageCount,
                    downloadedPages = 0
                )
                // 只有不存在时才插入
                val result = dao.insertIfNotExists(entity)
                if (result != -1L) {
                    Log.d(TAG, "Added download task: ${illust.id}")
                    processQueue()
                } else {
                    Log.d(TAG, "Task already exists: ${illust.id}")
                }
            }.onFailure { Log.e(TAG, "Failed to add task: ${it.message}") }
        }
    }

    /**
     * 批量添加下载任务
     */
    fun addTasks(illusts: List<Illust>) {
        if (illusts.isEmpty()) return
        scope.launch {
            illusts.forEach { illust ->
                addTaskInternal(illust)
            }
            processQueue()
        }
    }

    private suspend fun addTaskInternal(illust: Illust) {
        val dao = dao ?: return
        runCatching {
            val pageCount = illust.page_count ?: illust.meta_pages?.size ?: 1
            val entity = DownloadTaskEntity(
                illustId = illust.id,
                title = illust.title ?: "",
                previewUrl = illust.previewUrl(),
                width = illust.width,
                height = illust.height,
                userId = illust.user?.id ?: 0L,
                userName = illust.user?.name ?: "",
                userAvatarUrl = illust.user?.profile_image_urls?.findAvatarUrl(),
                illustJson = gson.toJson(illust),
                status = DownloadStatus.PENDING,
                totalPages = pageCount,
                downloadedPages = 0
            )
            dao.insertIfNotExists(entity)
        }
    }

    /**
     * 重试失败的任务
     */
    fun retryTask(illustId: Long) {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                dao.updateStatus(illustId, DownloadStatus.PENDING, null)
                Log.d(TAG, "Retry task: $illustId")
                processQueue()
            }
        }
    }

    /**
     * 重试所有失败的任务
     */
    fun retryAllFailed() {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                val failedTasks = dao.getByStatuses(listOf(DownloadStatus.FAILED))
                failedTasks.forEach { task ->
                    dao.updateStatus(task.illustId, DownloadStatus.PENDING, null)
                }
                Log.d(TAG, "Retry all failed tasks: ${failedTasks.size}")
                processQueue()
            }
        }
    }

    /**
     * 删除任务
     */
    fun deleteTask(illustId: Long) {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                dao.delete(illustId)
                Log.d(TAG, "Deleted task: $illustId")
            }
        }
    }

    /**
     * 清空已完成的任务
     */
    fun clearCompleted() {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                dao.deleteByStatus(DownloadStatus.COMPLETED)
                Log.d(TAG, "Cleared completed tasks")
            }
        }
    }

    /**
     * 清空所有任务
     */
    fun clearAll() {
        val dao = dao ?: return
        scope.launch {
            runCatching {
                dao.deleteAll()
                Log.d(TAG, "Cleared all tasks")
            }
        }
    }

    // ==================== 数据访问 ====================

    fun getAllTasksFlow(): Flow<List<DownloadTaskEntity>> {
        return dao?.getAllFlow() ?: emptyFlow()
    }

    fun getPendingTasksFlow(): Flow<List<DownloadTaskEntity>> {
        return dao?.getByStatusFlow(DownloadStatus.PENDING) ?: emptyFlow()
    }

    // ==================== 下载处理 ====================

    private fun processQueue() {
        scope.launch {
            downloadMutex.withLock {
                if (_isProcessing.value) return@launch

                val dao = dao ?: return@launch
                val context = appContext ?: return@launch

                val pendingTasks = dao.getByStatuses(listOf(DownloadStatus.PENDING))
                if (pendingTasks.isEmpty()) {
                    Log.d(TAG, "No pending tasks")
                    return@launch
                }

                _isProcessing.value = true
                Log.d(TAG, "Processing ${pendingTasks.size} pending tasks")

                for (task in pendingTasks) {
                    _currentDownloadId.value = task.illustId
                    downloadTask(context, task)
                }

                _currentDownloadId.value = null
                _isProcessing.value = false
                Log.d(TAG, "Queue processing completed")
            }
        }
    }

    private suspend fun downloadTask(context: Context, task: DownloadTaskEntity) {
        val dao = dao ?: return

        try {
            // 更新状态为下载中
            dao.updateStatus(task.illustId, DownloadStatus.DOWNLOADING)

            // 解析 Illust 对象
            val illust = gson.fromJson(task.illustJson, Illust::class.java)
                ?: throw Exception("Failed to parse illust JSON")

            // 获取所有图片 URL
            val imageUrls = getImageUrls(illust)
            val totalPages = imageUrls.size

            var downloadedPages = task.downloadedPages

            // 下载每一页
            for (i in downloadedPages until totalPages) {
                val url = imageUrls[i]
                val fileName = if (totalPages > 1) {
                    "pixiv_${illust.id}_p$i"
                } else {
                    "pixiv_${illust.id}"
                }

                val result = downloadImage(context, url, fileName)
                if (result.isSuccess) {
                    downloadedPages++
                    dao.updateProgress(task.illustId, DownloadStatus.DOWNLOADING, downloadedPages)
                    Log.d(TAG, "Downloaded page $downloadedPages/$totalPages for ${task.illustId}")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Download failed")
                }
            }

            // 标记完成
            dao.markCompleted(task.illustId, DownloadStatus.COMPLETED, downloadedPages)
            Log.d(TAG, "Completed download: ${task.illustId}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download ${task.illustId}: ${e.message}")
            dao.updateStatus(task.illustId, DownloadStatus.FAILED, e.message)
        }
    }

    private fun getImageUrls(illust: Illust): List<String> {
        val metaPages = illust.meta_pages
        return if (!metaPages.isNullOrEmpty()) {
            // 多图
            metaPages.mapNotNull { page ->
                page.image_urls?.original
                    ?: page.image_urls?.large
                    ?: page.image_urls?.medium
            }
        } else {
            // 单图
            listOf(illust.maxUrl() ?: illust.previewUrl())
        }
    }

    private suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "PixivAndroidApp/5.0.234 (Android 11; Pixel 5)")
                .build()

            val response = PixivClient.imageClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val inputStream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            response.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image"))
            }

            saveToGallery(context, bitmap, fileName)
            bitmap.recycle()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun saveToGallery(context: Context, bitmap: android.graphics.Bitmap, fileName: String) {
        val format = if (fileName.endsWith(".png", ignoreCase = true)) {
            android.graphics.Bitmap.CompressFormat.PNG
        } else {
            android.graphics.Bitmap.CompressFormat.JPEG
        }

        val mimeType = if (format == android.graphics.Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val extension = if (format == android.graphics.Bitmap.CompressFormat.PNG) ".png" else ".jpg"
        val finalFileName = if (fileName.contains(".")) fileName else "$fileName$extension"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/JCStaff")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(format, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "JCStaff")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val file = File(appDir, finalFileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(format, 100, outputStream)
            }

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )
        }
    }
}
