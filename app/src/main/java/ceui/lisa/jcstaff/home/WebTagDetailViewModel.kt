package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivWebScraper
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.WebIllust
import ceui.lisa.jcstaff.network.WebNovel
import ceui.lisa.jcstaff.network.WebPixpedia
import ceui.lisa.jcstaff.network.WebSearchPopular
import ceui.lisa.jcstaff.network.WebTagTranslation
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 网页版标签详情 ViewModel
 * 使用 /ajax/search/top 和 /ajax/search/tags API
 */
class WebTagDetailViewModel(
    private val tag: Tag
) : ViewModel() {

    private val _state = MutableStateFlow(WebTagDetailUiState(tag = tag))
    val state: StateFlow<WebTagDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val tagName = tag.name ?: return@launch

            // 并行请求两个 API
            val searchDeferred = async {
                PixivWebScraper.searchTagTop(tagName, forceRefresh = forceRefresh)
            }
            val tagInfoDeferred = async {
                PixivWebScraper.getTagInfo(tagName, forceRefresh = forceRefresh)
            }

            val searchResult = searchDeferred.await()
            val tagInfoResult = tagInfoDeferred.await()

            // 处理搜索结果
            searchResult.fold(
                onSuccess = { response ->
                    val body = response.body
                    val illusts = body?.illust?.data?.map { it.toIllust() } ?: emptyList()
                    val manga = body?.manga?.data?.map { it.toIllust() } ?: emptyList()
                    val novels = body?.novel?.data?.map { it.toNovel() } ?: emptyList()

                    // 存储到 ObjectStore
                    (illusts + manga).forEach { illust ->
                        ObjectStore.put(illust)
                        illust.user?.let { ObjectStore.put(it) }
                    }

                    _state.update { current ->
                        current.copy(
                            illusts = illusts,
                            manga = manga,
                            novels = novels,
                            illustTotal = body?.illust?.total ?: 0,
                            mangaTotal = body?.manga?.total ?: 0,
                            novelTotal = body?.novel?.total ?: 0,
                            popular = body?.popular,
                            relatedTags = body?.relatedTags ?: emptyList(),
                            tagTranslations = body?.tagTranslation ?: emptyMap()
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )

            // 处理标签信息
            tagInfoResult.fold(
                onSuccess = { response ->
                    val body = response.body
                    _state.update { current ->
                        current.copy(
                            pixpedia = body?.pixpedia,
                            parentTag = body?.pixpedia?.parentTag,
                            siblingsTags = body?.pixpedia?.siblingsTags ?: emptyList(),
                            childrenTags = body?.pixpedia?.childrenTags ?: emptyList(),
                            translatedName = body?.tagTranslation?.get(tagName)
                        )
                    }
                },
                onFailure = { /* 标签信息失败不影响主要内容 */ }
            )

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    companion object {
        fun factory(tag: Tag) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WebTagDetailViewModel(tag) as T
            }
        }
    }
}

data class WebTagDetailUiState(
    val tag: Tag,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 搜索结果
    val illusts: List<Illust> = emptyList(),
    val manga: List<Illust> = emptyList(),
    val novels: List<Novel> = emptyList(),
    val illustTotal: Int = 0,
    val mangaTotal: Int = 0,
    val novelTotal: Int = 0,
    val popular: WebSearchPopular? = null,
    val relatedTags: List<String> = emptyList(),
    val tagTranslations: Map<String, WebTagTranslation> = emptyMap(),
    // 标签信息
    val pixpedia: WebPixpedia? = null,
    val parentTag: String? = null,
    val siblingsTags: List<String> = emptyList(),
    val childrenTags: List<String> = emptyList(),
    val translatedName: WebTagTranslation? = null
) {
    val hasPixpedia: Boolean get() = pixpedia?.abstract != null
    val hasRelatedTags: Boolean get() = relatedTags.isNotEmpty() || siblingsTags.isNotEmpty() || childrenTags.isNotEmpty()
}
