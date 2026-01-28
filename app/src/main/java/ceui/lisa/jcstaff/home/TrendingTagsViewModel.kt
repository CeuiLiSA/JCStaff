package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class TrendingTagsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TrendingTagsUiState())
    val state: StateFlow<TrendingTagsUiState> = _state.asStateFlow()

    init {
        loadIllustTags()
        loadNovelTags()
    }

    fun loadIllustTags() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isIllustLoading = true, illustError = null)

            val cached = PixivClient.getFromStaleCache(
                path = "/v1/trending-tags/illust",
                queryParams = mapOf("filter" to "for_ios"),
                clazz = TrendingTagsResponse::class.java
            )
            if (cached != null) {
                storeTrendingTags(cached.trend_tags)
                _state.value = _state.value.copy(
                    illustTags = cached.trend_tags,
                    isIllustLoading = false
                )
            }

            try {
                val response = PixivClient.pixivApi.getTrendingTags()
                storeTrendingTags(response.trend_tags)
                _state.value = _state.value.copy(
                    illustTags = response.trend_tags,
                    isIllustLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isIllustLoading = false,
                    illustError = if (_state.value.illustTags.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
        }
    }

    fun loadNovelTags() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isNovelLoading = true, novelError = null)

            val cached = PixivClient.getFromStaleCache(
                path = "/v1/trending-tags/novel",
                queryParams = mapOf("filter" to "for_ios"),
                clazz = TrendingTagsResponse::class.java
            )
            if (cached != null) {
                storeTrendingTags(cached.trend_tags)
                _state.value = _state.value.copy(
                    novelTags = cached.trend_tags,
                    isNovelLoading = false
                )
            }

            try {
                val response = PixivClient.pixivApi.getTrendingNovelTags()
                storeTrendingTags(response.trend_tags)
                _state.value = _state.value.copy(
                    novelTags = response.trend_tags,
                    isNovelLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isNovelLoading = false,
                    novelError = if (_state.value.novelTags.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
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
