package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecommendedNovelsViewModel : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v1/novel/recommended",
            queryParams = mapOf(
                "include_ranking_illusts" to "false",
                "filter" to "for_ios"
            )
        ),
        responseClass = NovelResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getRecommendedNovels() },
        loadNextPage = { url -> PixivClient.pixivApi.getNextPageNovels(url) },
        extractItems = { it.novels },
        extractNextUrl = { it.next_url },
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
