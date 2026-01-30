package ceui.lisa.jcstaff.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.shouldFetch
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserDetailResponse
import ceui.lisa.jcstaff.network.UserProfile
import ceui.lisa.jcstaff.network.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户主页状态
 */
data class UserProfileState(
    val user: User? = null,
    val profile: UserProfile? = null,
    val workspace: Workspace? = null,
    val illusts: List<Illust> = emptyList(),
    val isLoadingProfile: Boolean = true,
    val isLoadingIllusts: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isFollowing: Boolean = false,
    val profileError: String? = null,
    val illustsError: String? = null,
    val nextUrl: String? = null
) {
    val isFollowed: Boolean get() = user?.is_followed == true
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

/**
 * 用户主页 ViewModel
 */
class UserProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private var currentUserId: Long = 0
    private var isLoaded = false

    /**
     * 加载用户数据
     */
    fun loadUser(userId: Long, forceRefresh: Boolean = false) {
        if (isLoaded && currentUserId == userId && !forceRefresh) return

        currentUserId = userId
        isLoaded = true

        loadProfile(userId, forceRefresh)
        loadIllusts(userId, forceRefresh)
    }

    private fun loadProfile(userId: Long, forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingProfile = true, profileError = null)

            val cacheConfig = CacheConfig(
                path = "/v1/user/detail",
                queryParams = mapOf("user_id" to userId.toString(), "filter" to "for_ios")
            )

            // 从缓存加载
            val cacheResult = cacheConfig.loadFromCache(UserDetailResponse::class.java)
            if (cacheResult != null) {
                cacheResult.data.user?.let { ObjectStore.put(it) }
                _state.value = _state.value.copy(
                    user = cacheResult.data.user,
                    profile = cacheResult.data.profile,
                    workspace = cacheResult.data.workspace,
                    isLoadingProfile = cacheResult.shouldFetch(forceRefresh)
                )
            }

            // 判断是否需要发网络请求
            if (!cacheResult.shouldFetch(forceRefresh)) {
                return@launch
            }

            try {
                val response = PixivClient.pixivApi.getUserDetail(userId)
                response.user?.let { ObjectStore.put(it) }

                _state.value = _state.value.copy(
                    user = response.user,
                    profile = response.profile,
                    workspace = response.workspace,
                    isLoadingProfile = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingProfile = false,
                    profileError = if (_state.value.user == null) e.message ?: "加载用户信息失败" else null
                )
            }
        }
    }

    private fun loadIllusts(userId: Long, forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingIllusts = true, illustsError = null)

            val cacheConfig = CacheConfig(
                path = "/v1/user/illusts",
                queryParams = mapOf("user_id" to userId.toString(), "type" to "illust", "filter" to "for_ios")
            )

            // 从缓存加载
            val cacheResult = cacheConfig.loadFromCache(IllustResponse::class.java)
            if (cacheResult != null) {
                storeIllusts(cacheResult.data.illusts)
                _state.value = _state.value.copy(
                    illusts = cacheResult.data.illusts,
                    isLoadingIllusts = cacheResult.shouldFetch(forceRefresh),
                    nextUrl = cacheResult.data.next_url
                )
            }

            // 判断是否需要发网络请求
            if (!cacheResult.shouldFetch(forceRefresh)) {
                return@launch
            }

            try {
                val response = PixivClient.pixivApi.getUserIllusts(userId)
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = response.illusts,
                    isLoadingIllusts = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingIllusts = false,
                    illustsError = if (_state.value.illusts.isEmpty()) e.message ?: "加载作品失败" else null
                )
            }
        }
    }

    /**
     * 加载更多作品
     */
    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            try {
                val response = PixivClient.getNextPage(nextUrl, IllustResponse::class.java)
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.illusts,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    illustsError = e.message ?: "加载更多失败"
                )
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        if (currentUserId > 0) {
            loadUser(currentUserId, forceRefresh = true)
        }
    }

    /**
     * 关注/取消关注用户
     */
    fun toggleFollow() {
        val user = _state.value.user ?: return
        val isCurrentlyFollowed = user.is_followed == true

        viewModelScope.launch {
            _state.value = _state.value.copy(isFollowing = true)

            try {
                if (isCurrentlyFollowed) {
                    PixivClient.pixivApi.unfollowUser(user.id)
                } else {
                    PixivClient.pixivApi.followUser(user.id)
                }

                // 更新状态
                val updatedUser = user.copy(is_followed = !isCurrentlyFollowed)
                ObjectStore.put(updatedUser)

                _state.value = _state.value.copy(
                    user = updatedUser,
                    isFollowing = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isFollowing = false)
            }
        }
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
