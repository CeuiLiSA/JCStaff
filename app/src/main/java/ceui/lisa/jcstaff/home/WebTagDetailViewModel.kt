package ceui.lisa.jcstaff.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivWebScraper
import ceui.lisa.jcstaff.network.Tag
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

    companion object {
        private const val TAG = "WebTagDetailVM"

        fun factory(tag: Tag) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WebTagDetailViewModel(tag) as T
            }
        }
    }

    private val _state = MutableStateFlow(WebTagDetailUiState(tag = tag))
    val state: StateFlow<WebTagDetailUiState> = _state.asStateFlow()

    init {
        Log.d(TAG, "init: tag=${tag.name}, translated=${tag.translated_name}")
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        if (_state.value.isLoading) {
            Log.d(TAG, "load: already loading, skip")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val tagName = tag.name
            if (tagName == null) {
                Log.e(TAG, "load: tagName is null!")
                _state.update { it.copy(isLoading = false, error = "Tag name is null") }
                return@launch
            }

            Log.d(TAG, "load: starting API calls for tag='$tagName', forceRefresh=$forceRefresh")

            // 并行请求两个 API
            val searchDeferred = async {
                Log.d(TAG, "searchTagTop: requesting...")
                PixivWebScraper.searchTagTop(tagName, forceRefresh = forceRefresh)
            }
            val tagInfoDeferred = async {
                Log.d(TAG, "getTagInfo: requesting...")
                PixivWebScraper.getTagInfo(tagName, forceRefresh = forceRefresh)
            }

            val searchResult = searchDeferred.await()
            val tagInfoResult = tagInfoDeferred.await()

            Log.d(TAG, "searchTagTop result: isSuccess=${searchResult.isSuccess}")
            Log.d(TAG, "getTagInfo result: isSuccess=${tagInfoResult.isSuccess}")

            // 处理搜索结果
            searchResult.fold(
                onSuccess = { response ->
                    val body = response.body
                    Log.d(TAG, "searchTagTop success: error=${response.error}, body=${body != null}")

                    if (body == null) {
                        Log.e(TAG, "searchTagTop: body is null!")
                        _state.update { it.copy(error = "Response body is null") }
                        return@fold
                    }

                    val illusts = body.illust?.data?.map { it.toIllust() } ?: emptyList()
                    val manga = body.manga?.data?.map { it.toIllust() } ?: emptyList()
                    val novels = body.novel?.data?.map { it.toNovel() } ?: emptyList()

                    Log.d(TAG, "searchTagTop parsed: illusts=${illusts.size}, manga=${manga.size}, novels=${novels.size}")
                    Log.d(TAG, "searchTagTop totals: illust=${body.illust?.total}, manga=${body.manga?.total}, novel=${body.novel?.total}")
                    Log.d(TAG, "searchTagTop popular: recent=${body.popular?.recent?.size}, permanent=${body.popular?.permanent?.size}")
                    Log.d(TAG, "searchTagTop relatedTags: ${body.relatedTags?.size}")

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
                            illustTotal = body.illust?.total ?: 0,
                            mangaTotal = body.manga?.total ?: 0,
                            novelTotal = body.novel?.total ?: 0,
                            popular = body.popular,
                            relatedTags = body.relatedTags ?: emptyList(),
                            tagTranslations = body.tagTranslation ?: emptyMap()
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "searchTagTop failed: ${e.message}", e)
                    _state.update { it.copy(error = e.message) }
                }
            )

            // 处理标签信息
            tagInfoResult.fold(
                onSuccess = { response ->
                    val body = response.body
                    Log.d(TAG, "getTagInfo success: pixpedia=${body?.pixpedia != null}, abstract=${body?.pixpedia?.abstract?.take(50)}")
                    Log.d(TAG, "getTagInfo: parentTag=${body?.pixpedia?.parentTag}, siblings=${body?.pixpedia?.siblingsTags?.size}, children=${body?.pixpedia?.childrenTags?.size}")

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
                onFailure = { e ->
                    Log.w(TAG, "getTagInfo failed (non-fatal): ${e.message}")
                }
            )

            _state.update { it.copy(isLoading = false) }
            Log.d(TAG, "load: complete, final state: illusts=${_state.value.illusts.size}, error=${_state.value.error}")
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh: called")
        load(forceRefresh = true)
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
