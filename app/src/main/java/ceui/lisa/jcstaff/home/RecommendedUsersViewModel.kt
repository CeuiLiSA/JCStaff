package ceui.lisa.jcstaff.home

import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse

typealias RecommendedUsersUiState = PagedState<UserPreview>

class RecommendedUsersViewModel : PagedViewModel<UserPreview, UserPreviewResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/user/recommended",
        queryParams = mapOf("filter" to "for_ios")
    ),
    responseClass = UserPreviewResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getRecommendedUsers() },
    onItemsLoaded = { previews ->
        previews.forEach { preview ->
            preview.user?.let { user -> ObjectStore.put(user) }
            preview.illusts.forEach { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
)
