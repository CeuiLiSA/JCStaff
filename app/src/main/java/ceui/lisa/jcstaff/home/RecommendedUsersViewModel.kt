package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

typealias RecommendedUsersUiState = PagedState<UserPreview>

class RecommendedUsersViewModel : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v1/user/recommended",
            queryParams = mapOf("filter" to "for_ios")
        ),
        responseClass = UserPreviewResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getRecommendedUsers() },
        onItemsLoaded = { previews -> storeUserPreviews(previews) }
    )

    val state: StateFlow<RecommendedUsersUiState> = loader.state

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

    private fun storeUserPreviews(previews: List<UserPreview>) {
        previews.forEach { preview ->
            preview.user?.let { user -> ObjectStore.put(user) }
            preview.illusts.forEach { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
}
