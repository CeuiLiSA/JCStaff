package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.HomeIllustResponse
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recommendedIllusts: List<Illust> = emptyList(),
    val followingIllusts: List<Illust> = emptyList(),
    val isLoadingRecommended: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val isLoadingMoreRecommended: Boolean = false,
    val isLoadingMoreFollowing: Boolean = false,
    val recommendedError: String? = null,
    val followingError: String? = null,
    val recommendedNextUrl: String? = null,
    val followingNextUrl: String? = null
) {
    val canLoadMoreRecommended: Boolean get() = recommendedNextUrl != null && !isLoadingMoreRecommended
    val canLoadMoreFollowing: Boolean get() = followingNextUrl != null && !isLoadingMoreFollowing
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecommendedIllusts()
        loadFollowingIllusts()
    }

    fun loadRecommendedIllusts() {
        viewModelScope.launch {
            // Stale-while-revalidate: 先从缓存加载旧数据
            val cachedResponse = PixivClient.getFromStaleCache(
                path = "/v1/illust/recommended",
                queryParams = mapOf(
                    "content_type" to "illust",
                    "include_ranking_label" to "true",
                    "filter" to "for_ios"
                ),
                clazz = HomeIllustResponse::class.java
            )

            if (cachedResponse != null) {
                // 有缓存，立即显示（不显示 loading）
                val illusts = cachedResponse.displayList
                storeIllusts(illusts)
                _uiState.value = _uiState.value.copy(
                    recommendedIllusts = illusts,
                    recommendedNextUrl = cachedResponse.next_url,
                    recommendedError = null
                )
            } else {
                // 无缓存，显示 loading
                _uiState.value = _uiState.value.copy(
                    isLoadingRecommended = true,
                    recommendedError = null
                )
            }

            // 发起网络请求获取最新数据
            try {
                val response = PixivClient.pixivApi.getRecommendedIllusts()
                val illusts = response.displayList

                // 存入 ObjectStore
                storeIllusts(illusts)

                _uiState.value = _uiState.value.copy(
                    recommendedIllusts = illusts,
                    isLoadingRecommended = false,
                    recommendedNextUrl = response.next_url
                )
            } catch (e: Exception) {
                // 如果已经有缓存数据，只显示错误信息但保留数据
                _uiState.value = _uiState.value.copy(
                    isLoadingRecommended = false,
                    recommendedError = if (_uiState.value.recommendedIllusts.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
        }
    }

    fun loadMoreRecommended() {
        val nextUrl = _uiState.value.recommendedNextUrl ?: return
        if (_uiState.value.isLoadingMoreRecommended) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreRecommended = true)
            try {
                val response = PixivClient.pixivApi.getNextPageHomeIllusts(nextUrl)
                val newIllusts = response.displayList

                storeIllusts(newIllusts)

                _uiState.value = _uiState.value.copy(
                    recommendedIllusts = _uiState.value.recommendedIllusts + newIllusts,
                    isLoadingMoreRecommended = false,
                    recommendedNextUrl = response.next_url
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreRecommended = false,
                    recommendedError = e.message ?: "加载更多失败"
                )
            }
        }
    }

    fun loadFollowingIllusts() {
        viewModelScope.launch {
            // Stale-while-revalidate: 先从缓存加载旧数据
            val cachedResponse = PixivClient.getFromStaleCache(
                path = "/v2/illust/follow",
                queryParams = mapOf("restrict" to "public"),
                clazz = IllustResponse::class.java
            )

            if (cachedResponse != null) {
                // 有缓存，立即显示（不显示 loading）
                val illusts = cachedResponse.illusts
                storeIllusts(illusts)
                _uiState.value = _uiState.value.copy(
                    followingIllusts = illusts,
                    followingNextUrl = cachedResponse.next_url,
                    followingError = null
                )
            } else {
                // 无缓存，显示 loading
                _uiState.value = _uiState.value.copy(
                    isLoadingFollowing = true,
                    followingError = null
                )
            }

            // 发起网络请求获取最新数据
            try {
                val response = PixivClient.pixivApi.getFollowingIllusts()
                val illusts = response.illusts

                // 存入 ObjectStore
                storeIllusts(illusts)

                _uiState.value = _uiState.value.copy(
                    followingIllusts = illusts,
                    isLoadingFollowing = false,
                    followingNextUrl = response.next_url
                )
            } catch (e: Exception) {
                // 如果已经有缓存数据，只显示错误信息但保留数据
                _uiState.value = _uiState.value.copy(
                    isLoadingFollowing = false,
                    followingError = if (_uiState.value.followingIllusts.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
        }
    }

    fun loadMoreFollowing() {
        val nextUrl = _uiState.value.followingNextUrl ?: return
        if (_uiState.value.isLoadingMoreFollowing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreFollowing = true)
            try {
                val response = PixivClient.pixivApi.getNextPageIllusts(nextUrl)
                val newIllusts = response.illusts

                storeIllusts(newIllusts)

                _uiState.value = _uiState.value.copy(
                    followingIllusts = _uiState.value.followingIllusts + newIllusts,
                    isLoadingMoreFollowing = false,
                    followingNextUrl = response.next_url
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreFollowing = false,
                    followingError = e.message ?: "加载更多失败"
                )
            }
        }
    }

    /**
     * 将插画列表存入 ObjectStore
     * 同时存储关联的 User
     */
    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user ->
                ObjectStore.put(user)
            }
        }
    }

    fun refresh() {
        loadRecommendedIllusts()
        loadFollowingIllusts()
    }
}
