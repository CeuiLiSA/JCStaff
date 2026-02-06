package ceui.lisa.jcstaff.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSearchParams(
    val tags: List<Tag> = emptyList()
) {
    val searchWord: String get() = tags.mapNotNull { it.name }.joinToString(" ")
}

class TagUserSearchViewModel(
    initialTag: Tag
) : ViewModel() {

    private val _searchParams = MutableStateFlow(UserSearchParams(tags = listOf(initialTag)))
    val searchParams: StateFlow<UserSearchParams> = _searchParams.asStateFlow()

    private var loader: PagedDataLoader<UserPreview, UserPreviewResponse>? = null
    private var stateCollectionJob: Job? = null
    private val _pagedState = MutableStateFlow(PagedState<UserPreview>())
    val pagedState: StateFlow<PagedState<UserPreview>> = _pagedState.asStateFlow()

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

    fun search() {
        val params = _searchParams.value
        if (params.searchWord.isBlank()) return

        stateCollectionJob?.cancel()
        loader = PagedDataLoader(
            cacheConfig = null,
            responseClass = UserPreviewResponse::class.java,
            loadFirstPage = {
                PixivClient.pixivApi.searchUsers(word = params.searchWord)
            },
            onItemsLoaded = { storeUsers(it) },
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

    private fun storeUsers(previews: List<UserPreview>) {
        previews.forEach { preview ->
            preview.user?.let { user -> ObjectStore.put(user) }
            preview.illusts.forEach { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }

    companion object {
        fun factory(initialTag: Tag) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagUserSearchViewModel(initialTag) as T
            }
        }
    }
}
