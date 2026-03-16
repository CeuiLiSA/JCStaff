package ceui.lisa.jcstaff.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.CollectionSummary
import ceui.lisa.jcstaff.network.CollectionTagGroup
import ceui.lisa.jcstaff.network.PixivWebScraper
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
                val pageData = PixivWebScraper.getCollectionPageData(forceRefresh = forceRefresh)
                    .getOrThrow()

                _state.update {
                    it.copy(
                        recommendCollections = pageData.recommendCollections,
                        everyoneCollections = pageData.everyoneCollections,
                        tagGroups = pageData.tagGroups,
                        isLoading = false,
                        error = null
                    )
                }

                // 异步获取封面图
                launch {
                    val allCollections = mutableListOf<CollectionSummary>()
                    allCollections.addAll(pageData.recommendCollections)
                    allCollections.addAll(pageData.everyoneCollections)
                    pageData.tagGroups.forEach { allCollections.addAll(it.collections) }

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
    val tagGroups: List<CollectionTagGroup> = emptyList(),
    val coverUrls: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = recommendCollections.isEmpty() && everyoneCollections.isEmpty() && tagGroups.isEmpty()
}
