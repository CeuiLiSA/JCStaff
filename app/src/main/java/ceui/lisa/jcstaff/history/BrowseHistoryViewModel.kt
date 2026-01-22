package ceui.lisa.jcstaff.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.BrowseHistoryManager
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 浏览历史状态
 */
data class BrowseHistoryState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isEmpty: Boolean get() = illusts.isEmpty() && !isLoading
}

/**
 * 浏览历史 ViewModel
 *
 * 观察 Room Flow，自动响应数据变化
 */
class BrowseHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(BrowseHistoryState())
    val state: StateFlow<BrowseHistoryState> = _state.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            BrowseHistoryManager.getHistoryFlow()
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载历史失败"
                    )
                }
                .collect { illusts ->
                    _state.value = _state.value.copy(
                        illusts = illusts,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearAll() {
        BrowseHistoryManager.clearAll()
    }

    /**
     * 删除指定历史记录
     */
    fun delete(illustId: Long) {
        BrowseHistoryManager.delete(illustId)
    }
}
