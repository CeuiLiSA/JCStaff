package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient

/**
 * 最新插画/漫画 ViewModel
 * contentType: "illust" 或 "manga"
 */
class LatestIllustsViewModel(contentType: String = "illust") : PagedViewModel<Illust, IllustResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/illust/new",
        queryParams = mapOf("content_type" to contentType, "filter" to "for_ios")
    ),
    responseClass = IllustResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getLatestIllusts(contentType) },
    onItemsLoaded = { illusts ->
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
) {
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
class LatestNovelsViewModel : PagedViewModel<Novel, NovelResponse>(
    cacheConfig = CacheConfig(path = "/v1/novel/new"),
    responseClass = NovelResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getLatestNovels() },
    onItemsLoaded = { novels ->
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
)
