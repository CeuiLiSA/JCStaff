package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.SpotlightArticle
import ceui.lisa.jcstaff.network.SpotlightResponse

class SpotlightViewModel : PagedViewModel<SpotlightArticle, SpotlightResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/spotlight/articles",
        queryParams = mapOf("category" to "all", "filter" to "for_android")
    ),
    responseClass = SpotlightResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getSpotlightArticles() }
)
