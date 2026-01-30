package ceui.lisa.jcstaff.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TagIllustSearchState(
    val tags: List<Tag> = emptyList(),
    val items: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null,
    val sort: SearchSort = SearchSort.DATE_DESC,
    val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS
) {
    val searchWord: String
        get() = tags.mapNotNull { it.name }.joinToString(" ")

    val canLoadMore: Boolean
        get() = nextUrl != null && !isLoadingMore

    // 兼容属性
    val illusts: List<Illust> get() = items
}

/**
 * 标签插画搜索 ViewModel
 */
class TagIllustSearchViewModel : ViewModel() {

    private val _state = MutableStateFlow(TagIllustSearchState())
    val state: StateFlow<TagIllustSearchState> = _state.asStateFlow()

    private var isInitialized = false

    fun init(initialTag: Tag, isPremium: Boolean = false) {
        if (isInitialized) return
        isInitialized = true
        val defaultSort = if (isPremium) SearchSort.POPULAR_DESC else SearchSort.POPULAR_PREVIEW
        _state.value = _state.value.copy(
            tags = listOf(initialTag),
            sort = defaultSort
        )
        search()
    }

    fun addTag(tag: Tag) {
        val current = _state.value.tags
        if (current.any { it.name == tag.name }) return
        _state.value = _state.value.copy(tags = current + tag)
        search()
    }

    fun removeTag(tag: Tag) {
        val current = _state.value.tags
        val updated = current.filter { it.name != tag.name }
        if (updated.isEmpty()) return
        _state.value = _state.value.copy(tags = updated)
        search()
    }

    fun setSort(sort: SearchSort) {
        if (_state.value.sort == sort) return
        _state.value = _state.value.copy(sort = sort)
        search()
    }

    fun setSearchTarget(target: SearchTarget) {
        if (_state.value.searchTarget == target) return
        _state.value = _state.value.copy(searchTarget = target)
        search()
    }

    fun search() {
        val word = _state.value.searchWord
        if (word.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                items = emptyList(),
                nextUrl = null
            )

            try {
                val response = if (_state.value.sort == SearchSort.POPULAR_PREVIEW) {
                    PixivClient.pixivApi.popularPreviewIllusts(
                        word = word,
                        searchTarget = _state.value.searchTarget.apiValue
                    )
                } else {
                    PixivClient.pixivApi.searchIllusts(
                        word = word,
                        sort = _state.value.sort.apiValue,
                        searchTarget = _state.value.searchTarget.apiValue
                    )
                }
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    items = response.illusts,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
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
                val response = PixivClient.getNextPage(nextUrl, IllustResponse::class.java)
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    items = _state.value.items + response.illusts,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() = search()

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
