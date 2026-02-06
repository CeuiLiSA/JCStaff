package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient

typealias NovelListUiState = PagedState<Novel>

class FollowingNovelsViewModel : PagedViewModel<Novel, NovelResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/novel/follow",
        queryParams = mapOf("restrict" to "public")
    ),
    responseClass = NovelResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getFollowingNovels() },
    onItemsLoaded = { novels ->
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
)
