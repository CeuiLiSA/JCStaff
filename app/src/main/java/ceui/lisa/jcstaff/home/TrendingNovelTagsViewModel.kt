package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.shouldFetch
import ceui.lisa.jcstaff.core.SimpleState
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.TrendingTag
import ceui.lisa.jcstaff.network.TrendingTagsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias TrendingNovelTagsState = SimpleState<TrendingTag>

/**
 * 热门小说标签 ViewModel
 */
class TrendingNovelTagsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TrendingNovelTagsState())
    val state: StateFlow<TrendingNovelTagsState> = _state.asStateFlow()

    private val cacheConfig = CacheConfig(
        path = "/v1/trending-tags/novel",
        queryParams = mapOf("filter" to "for_ios")
    )

    init {
        load(forceRefresh = false)
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 从缓存加载
            val cacheResult = cacheConfig.loadFromCache(TrendingTagsResponse::class.java)
            if (cacheResult != null) {
                storeTrendingTags(cacheResult.data.trend_tags)
                _state.value = _state.value.copy(
                    items = cacheResult.data.trend_tags,
                    isLoading = cacheResult.shouldFetch(forceRefresh)
                )
            }

            // 判断是否需要发网络请求
            if (!cacheResult.shouldFetch(forceRefresh)) {
                return@launch
            }

            // 从网络加载
            try {
                val response = PixivClient.pixivApi.getTrendingNovelTags()
                storeTrendingTags(response.trend_tags)
                _state.value = _state.value.copy(
                    items = response.trend_tags,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.items.isEmpty()) e.message ?: "加载失败" else null
                )
            }
        }
    }

    fun refresh() = load(forceRefresh = true)

    private fun storeTrendingTags(tags: List<TrendingTag>) {
        tags.forEach { tag ->
            tag.illust?.let { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
}
