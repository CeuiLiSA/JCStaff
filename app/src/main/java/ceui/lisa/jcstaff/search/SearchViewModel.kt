package ceui.lisa.jcstaff.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 搜索状态
 */
data class SearchState(
    val query: String = "",
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null,
    val recentSearches: List<String> = emptyList(),
    val hasSearched: Boolean = false
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

/**
 * 搜索 ViewModel
 */
class SearchViewModel : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    // 搜索历史（内存中保存，实际应用可使用 DataStore 持久化）
    private val searchHistory = mutableListOf<String>()

    init {
        _state.value = _state.value.copy(recentSearches = searchHistory.toList())
    }

    /**
     * 执行搜索
     */
    fun search(query: String) {
        if (query.isBlank()) return

        // 添加到搜索历史
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 10) {
            searchHistory.removeAt(searchHistory.lastIndex)
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                query = query,
                isLoading = true,
                error = null,
                illusts = emptyList(),
                nextUrl = null,
                recentSearches = searchHistory.toList(),
                hasSearched = true
            )

            try {
                val response = PixivClient.pixivApi.searchIllusts(query)
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = response.illusts,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }

    /**
     * 加载更多结果
     */
    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            try {
                val response = PixivClient.pixivApi.getNextPageIllusts(nextUrl)
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.illusts,
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

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        searchHistory.clear()
        _state.value = _state.value.copy(recentSearches = emptyList())
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
