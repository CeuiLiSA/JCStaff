package ceui.lisa.jcstaff.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TagUserSearchState(
    val tags: List<Tag> = emptyList(),
    val items: List<UserPreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val searchWord: String
        get() = tags.mapNotNull { it.name }.joinToString(" ")

    val canLoadMore: Boolean
        get() = nextUrl != null && !isLoadingMore
}

class TagUserSearchViewModel : ViewModel() {

    private val _state = MutableStateFlow(TagUserSearchState())
    val state: StateFlow<TagUserSearchState> = _state.asStateFlow()

    private var isInitialized = false

    fun init(initialTag: Tag) {
        if (isInitialized) return
        isInitialized = true
        _state.value = _state.value.copy(tags = listOf(initialTag))
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
                val response = PixivClient.pixivApi.searchUsers(word = word)
                storeUsers(response.user_previews)
                _state.value = _state.value.copy(
                    items = response.user_previews,
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
                val response = PixivClient.getNextPage(nextUrl, UserPreviewResponse::class.java)
                storeUsers(response.user_previews)
                _state.value = _state.value.copy(
                    items = _state.value.items + response.user_previews,
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

    private fun storeUsers(previews: List<UserPreview>) {
        previews.forEach { preview ->
            preview.user?.let { user -> ObjectStore.put(user) }
            preview.illusts.forEach { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
}
