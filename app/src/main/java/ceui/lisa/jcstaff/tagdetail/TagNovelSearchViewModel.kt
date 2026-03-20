package ceui.lisa.jcstaff.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NovelSearchParams(
    val tags: List<Tag> = emptyList(),
    val sort: SearchSort = SearchSort.DATE_DESC,
    val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val startDate: String? = null,
    val endDate: String? = null
) {
    val searchWord: String get() = tags.mapNotNull { it.name }.joinToString(" ")
}

class TagNovelSearchViewModel(
    initialTag: Tag,
    isPremium: Boolean = false
) : ViewModel() {

    private val _searchParams = MutableStateFlow(NovelSearchParams(
        tags = listOf(initialTag),
        sort = if (isPremium) SearchSort.POPULAR_DESC else SearchSort.POPULAR_PREVIEW
    ))
    val searchParams: StateFlow<NovelSearchParams> = _searchParams.asStateFlow()

    private var loader: PagedDataLoader<Novel, NovelResponse>? = null
    private var stateCollectionJob: Job? = null
    private val _pagedState = MutableStateFlow(PagedState<Novel>())
    val pagedState: StateFlow<PagedState<Novel>> = _pagedState.asStateFlow()

    init {
        search()
    }

    fun addTag(tag: Tag) {
        val current = _searchParams.value.tags
        if (current.any { it.name == tag.name }) return
        _searchParams.value = _searchParams.value.copy(tags = current + tag)
        search()
    }

    fun removeTag(tag: Tag) {
        val current = _searchParams.value.tags
        val updated = current.filter { it.name != tag.name }
        if (updated.isEmpty()) return
        _searchParams.value = _searchParams.value.copy(tags = updated)
        search()
    }

    fun setSort(sort: SearchSort) {
        if (_searchParams.value.sort == sort) return
        _searchParams.value = _searchParams.value.copy(sort = sort)
        search()
    }

    fun setSearchTarget(target: SearchTarget) {
        if (_searchParams.value.searchTarget == target) return
        _searchParams.value = _searchParams.value.copy(searchTarget = target)
        search()
    }

    fun setDateRange(startDate: String?, endDate: String?) {
        _searchParams.value = _searchParams.value.copy(startDate = startDate, endDate = endDate)
        search()
    }

    fun search() {
        val params = _searchParams.value
        if (params.searchWord.isBlank()) return

        stateCollectionJob?.cancel()
        loader = PagedDataLoader(
            cacheConfig = null,
            responseClass = NovelResponse::class.java,
            loadFirstPage = {
                if (params.sort == SearchSort.POPULAR_PREVIEW) {
                    PixivClient.pixivApi.popularPreviewNovels(
                        word = params.searchWord,
                        searchTarget = params.searchTarget.apiValue
                    )
                } else {
                    PixivClient.pixivApi.searchNovels(
                        word = params.searchWord,
                        sort = params.sort.apiValue,
                        searchTarget = params.searchTarget.apiValue,
                        startDate = params.startDate,
                        endDate = params.endDate
                    )
                }
            },
            onItemsLoaded = { storeNovels(it) },
            itemFilter = ContentFilterManager::shouldShow,
            scope = viewModelScope
        )
        stateCollectionJob = viewModelScope.launch {
            loader!!.state.collect { _pagedState.value = it }
        }
        viewModelScope.launch { loader?.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader?.loadMore() }
    }

    fun refresh() = search()

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }

    companion object {
        fun factory(initialTag: Tag, isPremium: Boolean = false) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagNovelSearchViewModel(initialTag, isPremium) as T
            }
        }
    }
}
