package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrendingUiState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

class TrendingViewModel : ViewModel() {

    private val _state = MutableStateFlow(TrendingUiState())
    val state: StateFlow<TrendingUiState> = _state.asStateFlow()

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
                path = "/v1/illust/ranking",
                queryParams = mapOf(
                    "mode" to "day",
                    "filter" to "for_ios"
                ),
                clazz = IllustResponse::class.java
            )

            if (cachedResponse != null) {
                val illusts = cachedResponse.illusts
                storeIllusts(illusts)
                _state.value = _state.value.copy(
                    illusts = illusts,
                    isLoading = false,
                    nextUrl = cachedResponse.next_url
                )
            }

            try {
                val response = PixivClient.pixivApi.getRankingIllusts(mode = "day")
                val illusts = response.illusts

                storeIllusts(illusts)

                _state.value = _state.value.copy(
                    illusts = illusts,
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
                val response = PixivClient.pixivApi.getNextPageIllusts(nextUrl)
                val newIllusts = response.illusts

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
}
