package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.CollectionResponse
import ceui.lisa.jcstaff.network.CollectionSummary
import ceui.lisa.jcstaff.network.CollectionTile
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivWebScraper
import ceui.lisa.jcstaff.network.WebIllust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val collectionId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = PixivWebScraper.getCollection(
                collectionId = collectionId,
                forceRefresh = forceRefresh
            )

            result.fold(
                onSuccess = { response ->
                    val body = response.body
                    val tiles = body?.data?.detail?.tiles ?: emptyList()
                    val webIllusts = body?.thumbnails?.illust ?: emptyList()
                    val illustMap = webIllusts.associateBy { it.id }

                    // Build ordered illust list from tiles
                    val orderedIllusts = tiles
                        .filter { it.type == "Work" && it.status == "Active" }
                        .mapNotNull { tile ->
                            illustMap[tile.workId]?.toIllust()
                        }

                    // Store illusts
                    orderedIllusts.forEach { illust ->
                        ObjectStore.put(illust)
                        illust.user?.let { user -> ObjectStore.put(user) }
                    }

                    // Extract collection info
                    val relatedCollections = body?.thumbnails?.collection ?: emptyList()
                    val userCollections = body?.data?.detail?.userCollections?.values?.toList() ?: emptyList()
                    val allRelated = (relatedCollections + userCollections).distinctBy { it.id }

                    val title = body?.extraData?.meta?.title
                        ?: body?.data?.detail?.tags?.tags?.firstOrNull()?.tag
                        ?: ""
                    val description = body?.extraData?.meta?.description ?: ""
                    val tags = body?.data?.detail?.tags?.tags?.mapNotNull { it.tag } ?: emptyList()
                    val authorName = body?.users?.firstOrNull()?.name ?: ""
                    val authorAvatar = body?.users?.firstOrNull()?.image

                    _state.update {
                        it.copy(
                            title = title,
                            description = description,
                            tags = tags,
                            authorName = authorName,
                            authorAvatar = authorAvatar,
                            illusts = orderedIllusts,
                            tiles = tiles,
                            relatedCollections = allRelated,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    companion object {
        fun factory(collectionId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CollectionViewModel(collectionId) as T
                }
            }
    }
}

data class CollectionUiState(
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val authorName: String = "",
    val authorAvatar: String? = null,
    val illusts: List<Illust> = emptyList(),
    val tiles: List<CollectionTile> = emptyList(),
    val relatedCollections: List<CollectionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
