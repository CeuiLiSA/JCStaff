package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 最新插画/漫画 ViewModel
 * contentType: "illust" 或 "manga"
 */
class LatestIllustsViewModel(private val contentType: String = "illust") : ViewModel() {

    private val _state = MutableStateFlow(FollowingUiState())
    val state: StateFlow<FollowingUiState> = _state.asStateFlow()

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
                path = "/v1/illust/new",
                queryParams = mapOf(
                    "content_type" to contentType,
                    "filter" to "for_ios"
                ),
                clazz = IllustResponse::class.java
            )

            if (cachedResponse != null) {
                storeIllusts(cachedResponse.illusts)
                _state.value = _state.value.copy(
                    illusts = cachedResponse.illusts,
                    isLoading = false,
                    nextUrl = cachedResponse.next_url
                )
            }

            try {
                val response = PixivClient.pixivApi.getLatestIllusts(contentType)
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    illusts = response.illusts,
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
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.illusts,
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
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

    companion object {
        fun factory(contentType: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LatestIllustsViewModel(contentType) as T
            }
        }
    }
}

/**
 * 最新小说 ViewModel
 */
class LatestNovelsViewModel : ViewModel() {

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
                path = "/v1/novel/new",
                queryParams = emptyMap(),
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
                val response = PixivClient.pixivApi.getLatestNovels()
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
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
