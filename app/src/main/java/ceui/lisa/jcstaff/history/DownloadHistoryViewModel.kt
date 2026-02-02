package ceui.lisa.jcstaff.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.DownloadStatus
import ceui.lisa.jcstaff.cache.DownloadTaskEntity
import ceui.lisa.jcstaff.core.DownloadTaskManager
import ceui.lisa.jcstaff.network.Illust
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 下载任务状态
 */
data class DownloadTaskState(
    val tasks: List<DownloadTaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isProcessing: Boolean = false,
    val currentDownloadId: Long? = null
) {
    val isEmpty: Boolean get() = tasks.isEmpty() && !isLoading

    val pendingCount: Int get() = tasks.count { it.status == DownloadStatus.PENDING }
    val downloadingCount: Int get() = tasks.count { it.status == DownloadStatus.DOWNLOADING }
    val completedCount: Int get() = tasks.count { it.status == DownloadStatus.COMPLETED }
    val failedCount: Int get() = tasks.count { it.status == DownloadStatus.FAILED }

    val pendingTasks: List<DownloadTaskEntity> get() = tasks.filter { it.status == DownloadStatus.PENDING }
    val downloadingTasks: List<DownloadTaskEntity> get() = tasks.filter { it.status == DownloadStatus.DOWNLOADING }
    val completedTasks: List<DownloadTaskEntity> get() = tasks.filter { it.status == DownloadStatus.COMPLETED }
    val failedTasks: List<DownloadTaskEntity> get() = tasks.filter { it.status == DownloadStatus.FAILED }

    // 将任务转换为 Illust 对象用于显示
    fun getIllusts(): List<Illust> {
        val gson = Gson()
        return tasks.mapNotNull { task ->
            runCatching { gson.fromJson(task.illustJson, Illust::class.java) }.getOrNull()
        }
    }
}

/**
 * 下载历史 ViewModel
 *
 * 管理下载任务列表和状态
 */
class DownloadHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(DownloadTaskState())
    val state: StateFlow<DownloadTaskState> = _state.asStateFlow()

    init {
        observeDownloadTasks()
    }

    private fun observeDownloadTasks() {
        viewModelScope.launch {
            combine(
                DownloadTaskManager.getAllTasksFlow(),
                DownloadTaskManager.isProcessing,
                DownloadTaskManager.currentDownloadId
            ) { tasks, isProcessing, currentDownloadId ->
                DownloadTaskState(
                    tasks = tasks,
                    isLoading = false,
                    error = null,
                    isProcessing = isProcessing,
                    currentDownloadId = currentDownloadId
                )
            }
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    fun retryTask(illustId: Long) {
        DownloadTaskManager.retryTask(illustId)
    }

    fun retryAllFailed() {
        DownloadTaskManager.retryAllFailed()
    }

    fun deleteTask(illustId: Long) {
        DownloadTaskManager.deleteTask(illustId)
    }

    fun clearCompleted() {
        DownloadTaskManager.clearCompleted()
    }

    fun clearAll() {
        DownloadTaskManager.clearAll()
    }
}
