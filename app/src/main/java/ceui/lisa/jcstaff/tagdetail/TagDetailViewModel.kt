package ceui.lisa.jcstaff.tagdetail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchSort(
    val apiValue: String,
    @StringRes val labelRes: Int,
    val premiumOnly: Boolean = false
) {
    DATE_DESC("date_desc", R.string.sort_date_desc),
    DATE_ASC("date_asc", R.string.sort_date_asc),
    POPULAR_DESC("popular_desc", R.string.sort_popular_desc, premiumOnly = true),
    POPULAR_MALE_DESC("popular_male_desc", R.string.sort_popular_male_desc, premiumOnly = true),
    POPULAR_FEMALE_DESC("popular_female_desc", R.string.sort_popular_female_desc, premiumOnly = true),
}

enum class SearchTarget(val apiValue: String, @StringRes val labelRes: Int) {
    PARTIAL_MATCH_FOR_TAGS("partial_match_for_tags", R.string.target_partial_tag),
    EXACT_MATCH_FOR_TAGS("exact_match_for_tags", R.string.target_exact_tag),
    TITLE_AND_CAPTION("title_and_caption", R.string.target_title_caption),
}

data class TagDetailState(
    val tags: List<Tag> = emptyList(),
    val illusts: List<Illust> = emptyList(),
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
}

class TagDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(TagDetailState())
    val state: StateFlow<TagDetailState> = _state.asStateFlow()

    private var isInitialized = false

    fun init(initialTag: Tag, isPremium: Boolean = false) {
        if (isInitialized) return
        isInitialized = true
        val defaultSort = if (isPremium) SearchSort.POPULAR_DESC else SearchSort.DATE_DESC
        _state.value = _state.value.copy(tags = listOf(initialTag), sort = defaultSort)
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
                illusts = emptyList(),
                nextUrl = null
            )

            try {
                val response = PixivClient.pixivApi.searchIllusts(
                    word = word,
                    sort = _state.value.sort.apiValue,
                    searchTarget = _state.value.searchTarget.apiValue
                )
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    illusts = response.illusts,
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
                val response = PixivClient.pixivApi.getNextPageIllusts(nextUrl)
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.illusts,
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
