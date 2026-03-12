package ceui.lisa.jcstaff.home

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

    private val _state = MutableStateFlow(CollectionDiscoveryUiState())
    val state: StateFlow<CollectionDiscoveryUiState> = _state.asStateFlow()

    init {
        load()
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

                val everyoneResult = everyoneDeferred.await()
                val everyoneCollections = everyoneResult.getOrNull()
                    ?.body?.thumbnails?.collection ?: emptyList()
                val everyoneTotal = everyoneResult.getOrNull()
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
                            ?.body?.thumbnails?.collection ?: emptyList()
                        val total = searchResult.getOrNull()
                            ?.body?.data?.total ?: 0
                        CollectionTagGroup(
                            tag = tag,
                            total = total,
                            collections = collections.take(20)
                        )
                    }
                }

                val tagGroups = tagGroupDeferreds.awaitAll()
                    .filter { it.collections.isNotEmpty() }

                _state.update {
                    it.copy(
                        recommendCollections = recommendCollections,
                        everyoneCollections = everyoneCollections.take(20),
                        everyoneTotal = everyoneTotal,
                        tagGroups = tagGroups,
                        recommendedTags = recommendedTags,
                        tagTranslation = tagTranslation,
                        isLoading = false,
                        error = null
                    )
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
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = recommendCollections.isEmpty() && everyoneCollections.isEmpty() && tagGroups.isEmpty()
}
