package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NovelListUiState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

class RecommendedNovelsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NovelListUiState())
    val state: StateFlow<NovelListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            val cachedResponse = PixivClient.getFromStaleCache(
                path = "/v1/novel/recommended",
                queryParams = mapOf(
                    "include_ranking_illusts" to "false",
                    "filter" to "for_ios"
                ),
                clazz = NovelResponse::class.java
            )

            if (cachedResponse != null) {
                storeNovels(cachedResponse.novels)
                _state.value = _state.value.copy(
                    novels = cachedResponse.novels,
                    isLoading = false,
                    nextUrl = cachedResponse.next_url
                )
            }

            try {
                val response = PixivClient.pixivApi.getRecommendedNovels()
                storeNovels(response.novels)
                _state.value = _state.value.copy(
                    novels = response.novels,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.novels.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
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
                val response = PixivClient.pixivApi.getNextPageNovels(nextUrl)
                storeNovels(response.novels)
                _state.value = _state.value.copy(
                    novels = _state.value.novels + response.novels,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }

    fun refresh() {
        load()
    }

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user ->
                ObjectStore.put(user)
            }
        }
    }
}
