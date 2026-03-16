package ceui.lisa.jcstaff.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.CollectionSummary
import ceui.lisa.jcstaff.network.CollectionTagGroup
import ceui.lisa.jcstaff.network.PixivWebScraper
import ceui.lisa.jcstaff.network.WebTagTranslation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionDiscoveryViewModel : ViewModel() {

    companion object {
        private const val TAG = "CollectionDiscovery"
    }

    private val _state = MutableStateFlow(CollectionDiscoveryUiState())
    val state: StateFlow<CollectionDiscoveryUiState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * 并发获取多个珍藏册的封面图 URL。
     * 通过 /ajax/collection/{id} 获取详情，取 thumbnails.illust[0].url 作为封面。
     * 结果由 ApiCacheManager 自动缓存，后续调用几乎零开销。
     */
    private suspend fun fetchCoverUrls(collections: List<CollectionSummary>): Map<String, String> {
        val deferreds = collections.mapNotNull { collection ->
            val id = collection.id ?: return@mapNotNull null
            viewModelScope.async {
                try {
                    val result = PixivWebScraper.getCollection(id)
                    val firstIllust = result.getOrNull()
                        ?.body?.thumbnails?.illust?.firstOrNull()
                    val url = firstIllust?.url
                    if (url != null) id to url else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch cover for collection $id: ${e.message}")
                    null
                }
            }
        }
        val coverUrls = deferreds.awaitAll().filterNotNull().toMap()
        Log.d(TAG, "fetchCoverUrls: ${coverUrls.size}/${collections.size} covers fetched")
        return coverUrls
    }

    fun load(forceRefresh: Boolean = false) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // 1. 并发：推荐珍藏册(HTML) + 全站最新(AJAX) + 推荐标签(AJAX)
                val recommendDeferred = async {
                    PixivWebScraper.getRecommendedCollections(forceRefresh = forceRefresh)
                }
                val everyoneDeferred = async {
                    PixivWebScraper.searchCollections(
                        word = "",
                        forceRefresh = forceRefresh
                    )
                }
                val tagsDeferred = async {
                    PixivWebScraper.getCollectionRecommendedTags()
                }

                val recommendCollections = recommendDeferred.await().getOrNull() ?: emptyList()

                val everyoneCollections = everyoneDeferred.await().getOrNull()
                    ?.body?.thumbnails?.collection?.take(20) ?: emptyList()
                val everyoneTotal = everyoneDeferred.await().getOrNull()
                    ?.body?.data?.total ?: 0

                val tagsResult = tagsDeferred.await()
                val recommendedTags = tagsResult.getOrNull()
                    ?.body?.recommendedTags ?: emptyList()
                val tagTranslation = tagsResult.getOrNull()
                    ?.body?.tagTranslation ?: emptyMap()

                // 2. 并发获取每个推荐标签的珍藏册
                val tagGroupDeferreds = recommendedTags.map { tag ->
                    async {
                        val searchResult = PixivWebScraper.searchCollections(word = tag)
                        val collections = searchResult.getOrNull()
                            ?.body?.thumbnails?.collection?.take(20) ?: emptyList()
                        val total = searchResult.getOrNull()
                            ?.body?.data?.total ?: 0
                        CollectionTagGroup(
                            tag = tag,
                            total = total,
                            collections = collections
                        )
                    }
                }

                val tagGroups = tagGroupDeferreds.awaitAll()
                    .filter { it.collections.isNotEmpty() }

                // 先展示数据（无封面），然后异步加载封面
                _state.update {
                    it.copy(
                        recommendCollections = recommendCollections,
                        everyoneCollections = everyoneCollections,
                        everyoneTotal = everyoneTotal,
                        tagGroups = tagGroups,
                        recommendedTags = recommendedTags,
                        tagTranslation = tagTranslation,
                        isLoading = false,
                        error = null
                    )
                }

                // 3. 异步获取封面图（每个 section 取前几个即可，避免过多请求）
                launch {
                    val allCollections = mutableListOf<CollectionSummary>()
                    allCollections.addAll(recommendCollections)
                    allCollections.addAll(everyoneCollections)
                    tagGroups.forEach { allCollections.addAll(it.collections) }

                    // 去重，只取前 60 个避免过多请求
                    val uniqueCollections = allCollections
                        .distinctBy { it.id }
                        .take(60)

                    val coverUrls = fetchCoverUrls(uniqueCollections)

                    if (coverUrls.isNotEmpty()) {
                        _state.update { currentState ->
                            currentState.copy(
                                coverUrls = currentState.coverUrls + coverUrls
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun refresh() {
        load(forceRefresh = true)
    }
}

data class CollectionDiscoveryUiState(
    val recommendCollections: List<CollectionSummary> = emptyList(),
    val everyoneCollections: List<CollectionSummary> = emptyList(),
    val everyoneTotal: Int = 0,
    val tagGroups: List<CollectionTagGroup> = emptyList(),
    val recommendedTags: List<String> = emptyList(),
    val tagTranslation: Map<String, WebTagTranslation> = emptyMap(),
    val coverUrls: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = recommendCollections.isEmpty() && everyoneCollections.isEmpty() && tagGroups.isEmpty()
}
