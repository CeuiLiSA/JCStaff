package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 排行榜 ViewModel
 * 特殊：支持日期切换，不使用 PagedDataLoader 以支持日期状态
 */
class RankingViewModel(
    private val mode: String,
    initialDate: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(
        PagedState<Illust>().copy(selectedDate = initialDate)
    )
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val response = PixivClient.pixivApi.getRankingIllusts(
                    mode = mode,
                    date = _state.value.selectedDate
                )
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    items = response.illusts,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.items.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
        }
    }

    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            try {
                val response = PixivClient.getNextPage(nextUrl, IllustResponse::class.java)
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    items = _state.value.items + response.illusts,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }

    fun setDate(date: String?) {
        if (date == _state.value.selectedDate) return
        _state.value = _state.value.copy(
            selectedDate = date,
            items = emptyList(),
            nextUrl = null
        )
        load()
    }

    fun refresh() {
        load()
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

    companion object {
        fun factory(mode: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RankingViewModel(mode) as T
            }
        }
    }
}

// 扩展 PagedState 用于排行榜的日期字段
private fun <T> PagedState<T>.copy(selectedDate: String?) = RankingUiState(
    items = this.items as List<Illust>,
    isLoading = this.isLoading,
    isLoadingMore = this.isLoadingMore,
    error = this.error,
    nextUrl = this.nextUrl,
    selectedDate = selectedDate
)

data class RankingUiState(
    val items: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null,
    val selectedDate: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore

    // 兼容旧代码
    val illusts: List<Illust> get() = items
}
