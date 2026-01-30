package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * 排行榜 ViewModel
 * 使用 PagedDataLoader + 独立的日期状态
 */
class RankingViewModel(
    private val mode: String,
    initialDate: String? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(initialDate)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val loader = PagedDataLoader(
        cacheConfigProvider = { buildCacheConfig() },
        responseClass = IllustResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getRankingIllusts(mode, _selectedDate.value) },
        onItemsLoaded = { storeIllusts(it) }
    )

    val state: StateFlow<RankingUiState> = combine(loader.state, _selectedDate) { pagedState, date ->
        RankingUiState(
            items = pagedState.items,
            isLoading = pagedState.isLoading,
            isLoadingMore = pagedState.isLoadingMore,
            error = pagedState.error,
            nextUrl = pagedState.nextUrl,
            selectedDate = date
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RankingUiState(selectedDate = initialDate))

    init {
        viewModelScope.launch { loader.load(forceRefresh = false) }
    }

    private fun buildCacheConfig(): CacheConfig {
        val queryParams = mutableMapOf("mode" to mode, "filter" to "for_ios")
        _selectedDate.value?.let { queryParams["date"] = it }
        return CacheConfig(path = "/v1/illust/ranking", queryParams = queryParams)
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun setDate(date: String?) {
        if (date == _selectedDate.value) return
        _selectedDate.value = date
        loader.reset()
        viewModelScope.launch { loader.load(forceRefresh = false) }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
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

data class RankingUiState(
    val items: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null,
    val selectedDate: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
    val illusts: List<Illust> get() = items
}
