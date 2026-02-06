package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient

class RecommendedNovelsViewModel : PagedViewModel<Novel, NovelResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/novel/recommended",
        queryParams = mapOf("include_ranking_illusts" to "false", "filter" to "for_ios")
    ),
    responseClass = NovelResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getRecommendedNovels() },
    onItemsLoaded = { novels ->
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
)
