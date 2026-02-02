package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivWebScraper
import ceui.lisa.jcstaff.network.WebRankingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 动图排行榜 ViewModel
 * 使用网页抓取获取数据
 */
class UgoiraRankingViewModel(
    private val mode: String = PixivWebScraper.RankingMode.DAILY
) : ViewModel() {

    private val _state = MutableStateFlow(UgoiraRankingUiState())
    val state: StateFlow<UgoiraRankingUiState> = _state.asStateFlow()

    private var currentPage = 1
    private var hasMorePages = true

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            if (forceRefresh) {
                currentPage = 1
                hasMorePages = true
                _state.update { it.copy(items = emptyList()) }
            }

            _state.update { it.copy(isLoading = true, error = null) }

            val result = PixivWebScraper.getUgoiraRanking(
                mode = mode,
                date = _state.value.selectedDate,
                page = 1
            )

            result.fold(
                onSuccess = { items ->
                    val illusts = items.map { it.toIllust() }
                    storeIllusts(illusts)
                    currentPage = 1
                    hasMorePages = items.size >= 50
                    _state.update {
                        it.copy(
                            items = illusts,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore || !hasMorePages) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }

            val nextPage = currentPage + 1
            val result = PixivWebScraper.getUgoiraRanking(
                mode = mode,
                date = _state.value.selectedDate,
                page = nextPage
            )

            result.fold(
                onSuccess = { items ->
                    val illusts = items.map { it.toIllust() }
                    storeIllusts(illusts)
                    currentPage = nextPage
                    hasMorePages = items.size >= 50
                    _state.update {
                        it.copy(
                            items = it.items + illusts,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun setDate(date: String?) {
        if (date == _state.value.selectedDate) return
        _state.update { it.copy(selectedDate = date) }
        load(forceRefresh = true)
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

    companion object {
        fun factory(mode: String = PixivWebScraper.RankingMode.DAILY) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UgoiraRankingViewModel(mode) as T
                }
            }
    }
}

data class UgoiraRankingUiState(
    val items: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedDate: String? = null
) {
    val canLoadMore: Boolean get() = !isLoadingMore && items.isNotEmpty()
    val illusts: List<Illust> get() = items
}
