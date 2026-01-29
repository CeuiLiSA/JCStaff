package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.SpotlightArticle
import ceui.lisa.jcstaff.network.SpotlightResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Spotlight (Pixiv 精选) ViewModel
 * 加载 Pixiv 官方精选文章
 */
class SpotlightViewModel : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v1/spotlight/articles",
            queryParams = mapOf("category" to "all", "filter" to "for_android")
        ),
        responseClass = SpotlightResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getSpotlightArticles() }
    )

    val state: StateFlow<PagedState<SpotlightArticle>> = loader.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loader.load() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }
}
