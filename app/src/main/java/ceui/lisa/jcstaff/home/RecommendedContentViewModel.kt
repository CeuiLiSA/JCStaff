package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.HomeIllustResponse
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecommendedContentViewModel(private val contentType: String) : ViewModel() {

    private val _state = MutableStateFlow(RecommendedUiState())
    val state: StateFlow<RecommendedUiState> = _state.asStateFlow()

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
                path = "/v1/$contentType/recommended",
                queryParams = mapOf(
                    "include_ranking_illusts" to "true",
                    "filter" to "for_ios"
                ),
                clazz = HomeIllustResponse::class.java
            )

            if (cachedResponse != null) {
                val illusts = cachedResponse.illusts
                val ranking = cachedResponse.ranking_illusts
                storeIllusts(illusts)
                storeIllusts(ranking)
                _state.value = _state.value.copy(
                    illusts = illusts,
                    rankingIllusts = ranking,
                    isLoading = false,
                    nextUrl = cachedResponse.next_url
                )
            }

            try {
                val response = PixivClient.pixivApi.getRecommendedContent(contentType)
                val illusts = response.illusts
                val ranking = response.ranking_illusts

                storeIllusts(illusts)
                storeIllusts(ranking)

                _state.value = _state.value.copy(
                    illusts = illusts,
                    rankingIllusts = ranking,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.illusts.isEmpty()) {
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
                val response = PixivClient.pixivApi.getNextPageHomeIllusts(nextUrl)
                val newIllusts = response.displayList

                storeIllusts(newIllusts)

                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + newIllusts,
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

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user ->
                ObjectStore.put(user)
            }
        }
    }

    companion object {
        fun factory(contentType: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecommendedContentViewModel(contentType) as T
            }
        }
    }
}
