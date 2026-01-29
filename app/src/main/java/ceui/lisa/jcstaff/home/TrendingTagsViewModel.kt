package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.TrendingTag
import ceui.lisa.jcstaff.network.TrendingTagsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrendingTagsUiState(
    val illustTags: List<TrendingTag> = emptyList(),
    val novelTags: List<TrendingTag> = emptyList(),
    val isIllustLoading: Boolean = false,
    val isNovelLoading: Boolean = false,
    val illustError: String? = null,
    val novelError: String? = null
)

/**
 * 热门标签 ViewModel
 * 特殊：同时管理插画和小说两种标签，不使用 PagedDataLoader
 */
class TrendingTagsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TrendingTagsUiState())
    val state: StateFlow<TrendingTagsUiState> = _state.asStateFlow()

    private val illustCacheConfig = CacheConfig(
        path = "/v1/trending-tags/illust",
        queryParams = mapOf("filter" to "for_ios")
    )

    private val novelCacheConfig = CacheConfig(
        path = "/v1/trending-tags/novel",
        queryParams = mapOf("filter" to "for_ios")
    )

    init {
        loadIllustTags()
        loadNovelTags()
    }

    fun loadIllustTags() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isIllustLoading = true, illustError = null)

            loadTags(
                cacheConfig = illustCacheConfig,
                apiCall = { PixivClient.pixivApi.getTrendingTags() },
                onSuccess = { tags ->
                    _state.value = _state.value.copy(
                        illustTags = tags,
                        isIllustLoading = false
                    )
                },
                onError = { error ->
                    _state.value = _state.value.copy(
                        isIllustLoading = false,
                        illustError = if (_state.value.illustTags.isEmpty()) error else null
                    )
                }
            )
        }
    }

    fun loadNovelTags() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isNovelLoading = true, novelError = null)

            loadTags(
                cacheConfig = novelCacheConfig,
                apiCall = { PixivClient.pixivApi.getTrendingNovelTags() },
                onSuccess = { tags ->
                    _state.value = _state.value.copy(
                        novelTags = tags,
                        isNovelLoading = false
                    )
                },
                onError = { error ->
                    _state.value = _state.value.copy(
                        isNovelLoading = false,
                        novelError = if (_state.value.novelTags.isEmpty()) error else null
                    )
                }
            )
        }
    }

    private suspend fun loadTags(
        cacheConfig: CacheConfig,
        apiCall: suspend () -> TrendingTagsResponse,
        onSuccess: (List<TrendingTag>) -> Unit,
        onError: (String) -> Unit
    ) {
        // 从缓存加载
        val cached = PixivClient.getFromStaleCache(
            path = cacheConfig.path,
            queryParams = cacheConfig.queryParams,
            clazz = TrendingTagsResponse::class.java
        )
        if (cached != null) {
            storeTrendingTags(cached.trend_tags)
            onSuccess(cached.trend_tags)
        }

        // 从网络加载
        try {
            val response = apiCall()
            storeTrendingTags(response.trend_tags)
            onSuccess(response.trend_tags)
        } catch (e: Exception) {
            onError(e.message ?: "加载失败")
        }
    }

    fun refresh() {
        loadIllustTags()
        loadNovelTags()
    }

    private fun storeTrendingTags(tags: List<TrendingTag>) {
        tags.forEach { tag ->
            tag.illust?.let { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
}
