package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.HomeIllustResponse
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 推荐内容 UI 状态，包含排行榜数据
 */
data class RecommendedUiState(
    val illusts: List<Illust> = emptyList(),
    val rankingIllusts: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

/**
 * 推荐插画/漫画 ViewModel
 * 特殊：包含 ranking_illusts 字段，不能用通用 PagedDataLoader
 */
class RecommendedContentViewModel(private val contentType: String) : ViewModel() {

    private val _state = MutableStateFlow(RecommendedUiState())
    val state: StateFlow<RecommendedUiState> = _state.asStateFlow()

    private val cacheConfig = CacheConfig(
        path = "/v1/$contentType/recommended",
        queryParams = mapOf(
            "include_ranking_illusts" to "true",
            "filter" to "for_ios"
        )
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 从缓存加载
            val cached = PixivClient.getFromStaleCache(
                path = cacheConfig.path,
                queryParams = cacheConfig.queryParams,
                clazz = HomeIllustResponse::class.java
            )
            if (cached != null) {
                storeIllusts(cached.illusts)
                storeIllusts(cached.ranking_illusts)
                _state.value = _state.value.copy(
                    illusts = cached.illusts,
                    rankingIllusts = cached.ranking_illusts,
                    isLoading = false,
                    nextUrl = cached.next_url
                )
            }

            // 从网络加载
            try {
                val response = PixivClient.pixivApi.getRecommendedContent(contentType)
                storeIllusts(response.illusts)
                storeIllusts(response.ranking_illusts)
                _state.value = _state.value.copy(
                    illusts = response.illusts,
                    rankingIllusts = response.ranking_illusts,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.illusts.isEmpty()) {
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
                val response = PixivClient.getNextPage(nextUrl, HomeIllustResponse::class.java)
                storeIllusts(response.displayList)
                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.displayList,
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
        fun factory(contentType: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecommendedContentViewModel(contentType) as T
            }
        }
    }
}
