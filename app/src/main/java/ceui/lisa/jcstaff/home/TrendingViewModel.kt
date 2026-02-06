package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient

typealias TrendingUiState = PagedState<Illust>

class TrendingViewModel : PagedViewModel<Illust, IllustResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/illust/ranking",
        queryParams = mapOf("mode" to "day", "filter" to "for_ios")
    ),
    responseClass = IllustResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getRankingIllusts(mode = "day") },
    onItemsLoaded = { illusts ->
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
)
