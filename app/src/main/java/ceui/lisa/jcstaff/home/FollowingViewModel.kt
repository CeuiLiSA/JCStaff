package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient

typealias FollowingUiState = PagedState<Illust>

class FollowingViewModel : PagedViewModel<Illust, IllustResponse>(
    cacheConfig = CacheConfig(
        path = "/v2/illust/follow",
        queryParams = mapOf("restrict" to "public")
    ),
    responseClass = IllustResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getFollowingIllusts() },
    onItemsLoaded = { illusts ->
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
)
