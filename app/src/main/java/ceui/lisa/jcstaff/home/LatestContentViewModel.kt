package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 最新插画/漫画 ViewModel
 * contentType: "illust" 或 "manga"
 */
class LatestIllustsViewModel(private val contentType: String = "illust") : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v1/illust/new",
            queryParams = mapOf(
                "content_type" to contentType,
                "filter" to "for_ios"
            )
        ),
        responseClass = IllustResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getLatestIllusts(contentType) },
        onItemsLoaded = { illusts -> storeIllusts(illusts) }
    )

    val state: StateFlow<FollowingUiState> = loader.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
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

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v1/novel/new",
            queryParams = emptyMap()
        ),
        responseClass = NovelResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getLatestNovels() },
        onItemsLoaded = { novels -> storeNovels(novels) }
    )

    val state: StateFlow<NovelListUiState> = loader.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
    }

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
