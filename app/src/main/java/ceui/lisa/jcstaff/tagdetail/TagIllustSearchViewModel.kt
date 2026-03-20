package ceui.lisa.jcstaff.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IllustSearchParams(
    val tags: List<Tag> = emptyList(),
    val sort: SearchSort = SearchSort.DATE_DESC,
    val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    val startDate: String? = null,
    val endDate: String? = null
) {
    val searchWord: String get() = tags.mapNotNull { it.name }.joinToString(" ")
}

class TagIllustSearchViewModel(
    initialTag: Tag,
    isPremium: Boolean = false
) : ViewModel() {

    private val _searchParams = MutableStateFlow(IllustSearchParams(
        tags = listOf(initialTag),
        sort = if (isPremium) SearchSort.POPULAR_DESC else SearchSort.POPULAR_PREVIEW
    ))
    val searchParams: StateFlow<IllustSearchParams> = _searchParams.asStateFlow()

    private var loader: PagedDataLoader<Illust, IllustResponse>? = null
    private var stateCollectionJob: Job? = null
    private val _pagedState = MutableStateFlow(PagedState<Illust>())
    val pagedState: StateFlow<PagedState<Illust>> = _pagedState.asStateFlow()

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
            responseClass = IllustResponse::class.java,
            loadFirstPage = {
                if (params.sort == SearchSort.POPULAR_PREVIEW) {
                    PixivClient.pixivApi.popularPreviewIllusts(
                        word = params.searchWord,
                        searchTarget = params.searchTarget.apiValue
                    )
                } else {
                    PixivClient.pixivApi.searchIllusts(
                        word = params.searchWord,
                        sort = params.sort.apiValue,
                        searchTarget = params.searchTarget.apiValue,
                        startDate = params.startDate,
                        endDate = params.endDate
                    )
                }
            },
            onItemsLoaded = { storeIllusts(it) },
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

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

    companion object {
        fun factory(initialTag: Tag, isPremium: Boolean = false) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagIllustSearchViewModel(initialTag, isPremium) as T
            }
        }
    }
}
