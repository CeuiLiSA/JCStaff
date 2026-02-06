package ceui.lisa.jcstaff.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.shouldFetch
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
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
    // Section previews
    val illusts: List<Illust> = emptyList(),
    val mangaList: List<Illust> = emptyList(),
    val novels: List<Novel> = emptyList(),
    val bookmarkedIllusts: List<Illust> = emptyList(),
    val bookmarkedNovels: List<Novel> = emptyList(),
    // Loading states
    val isLoadingProfile: Boolean = true,
    val isLoadingIllusts: Boolean = true,
    val isLoadingManga: Boolean = true,
    val isLoadingNovels: Boolean = true,
    val isLoadingBookmarkedIllusts: Boolean = true,
    val isLoadingBookmarkedNovels: Boolean = false,
    val bookmarkedNovelsLoadTriggered: Boolean = false,
    val isFollowing: Boolean = false,
    val profileError: String? = null,
) {
    val isFollowed: Boolean get() = user?.is_followed == true
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
     * 优先加载 profile，根据 profile 中的 total_xxx 字段决定是否加载对应内容
     */
    fun loadUser(userId: Long, forceRefresh: Boolean = false) {
        if (isLoaded && currentUserId == userId && !forceRefresh) return

        currentUserId = userId
        isLoaded = true

        // 先加载 profile，获取各板块的数量
        loadProfileFirst(userId, forceRefresh)
    }

    /**
     * 优先加载 profile，然后根据 total_xxx 决定是否加载其他内容
     */
    private fun loadProfileFirst(userId: Long, forceRefresh: Boolean) {
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
                    isLoadingProfile = cacheResult.shouldFetch(forceRefresh),
                    // 根据 profile 中的 count 设置 loading 状态
                    isLoadingIllusts = (cacheResult.data.profile?.total_illusts ?: 0) > 0,
                    isLoadingManga = (cacheResult.data.profile?.total_manga ?: 0) > 0,
                    isLoadingNovels = (cacheResult.data.profile?.total_novels ?: 0) > 0,
                    isLoadingBookmarkedIllusts = (cacheResult.data.profile?.total_illust_bookmarks_public ?: 0) > 0,
                    isLoadingBookmarkedNovels = false // 收藏小说没有 count 字段，先设为 false
                )
                // 使用缓存的 profile 触发内容加载
                loadContentBasedOnProfile(userId, cacheResult.data.profile, forceRefresh)
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
                    isLoadingProfile = false,
                    // 根据 profile 中的 count 设置 loading 状态
                    isLoadingIllusts = (response.profile?.total_illusts ?: 0) > 0,
                    isLoadingManga = (response.profile?.total_manga ?: 0) > 0,
                    isLoadingNovels = (response.profile?.total_novels ?: 0) > 0,
                    isLoadingBookmarkedIllusts = (response.profile?.total_illust_bookmarks_public ?: 0) > 0,
                    isLoadingBookmarkedNovels = false
                )

                // 如果没有缓存命中，使用网络响应的 profile 触发内容加载
                if (cacheResult == null) {
                    loadContentBasedOnProfile(userId, response.profile, forceRefresh)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingProfile = false,
                    isLoadingIllusts = false,
                    isLoadingManga = false,
                    isLoadingNovels = false,
                    isLoadingBookmarkedIllusts = false,
                    isLoadingBookmarkedNovels = false,
                    profileError = if (_state.value.user == null) e.message ?: "加载用户信息失败" else null
                )
            }
        }
    }

    /**
     * 根据 profile 中的 count 决定是否加载各板块内容
     */
    private fun loadContentBasedOnProfile(userId: Long, profile: UserProfile?, forceRefresh: Boolean) {
        val totalIllusts = profile?.total_illusts ?: 0
        val totalManga = profile?.total_manga ?: 0
        val totalNovels = profile?.total_novels ?: 0
        val totalBookmarkedIllusts = profile?.total_illust_bookmarks_public ?: 0

        // 只有 count > 0 才发起网络请求
        if (totalIllusts > 0) {
            loadIllusts(userId, forceRefresh)
        }
        if (totalManga > 0) {
            loadManga(userId, forceRefresh)
        }
        if (totalNovels > 0) {
            loadNovels(userId)
        }
        if (totalBookmarkedIllusts > 0) {
            loadBookmarkedIllusts(userId)
        }
        // 收藏小说没有 count 字段，滚动到可见时才加载
    }

    /**
     * 触发收藏小说加载（滚动到可见时调用）
     */
    fun triggerBookmarkedNovelsLoad() {
        if (_state.value.bookmarkedNovelsLoadTriggered) return
        _state.value = _state.value.copy(bookmarkedNovelsLoadTriggered = true)
        if (currentUserId > 0) {
            loadBookmarkedNovels(currentUserId)
        }
    }

    private fun loadIllusts(userId: Long, forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingIllusts = true)

            val cacheConfig = CacheConfig(
                path = "/v1/user/illusts",
                queryParams = mapOf("user_id" to userId.toString(), "type" to "illust", "filter" to "for_ios")
            )

            val cacheResult = cacheConfig.loadFromCache(IllustResponse::class.java)
            if (cacheResult != null) {
                storeIllusts(cacheResult.data.illusts)
                _state.value = _state.value.copy(
                    illusts = cacheResult.data.illusts,
                    isLoadingIllusts = cacheResult.shouldFetch(forceRefresh)
                )
            }

            if (!cacheResult.shouldFetch(forceRefresh)) return@launch

            try {
                val response = PixivClient.pixivApi.getUserIllusts(userId, type = "illust")
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    illusts = response.illusts,
                    isLoadingIllusts = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingIllusts = false)
            }
        }
    }

    private fun loadManga(userId: Long, forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingManga = true)

            val cacheConfig = CacheConfig(
                path = "/v1/user/illusts",
                queryParams = mapOf("user_id" to userId.toString(), "type" to "manga", "filter" to "for_ios")
            )

            val cacheResult = cacheConfig.loadFromCache(IllustResponse::class.java)
            if (cacheResult != null) {
                storeIllusts(cacheResult.data.illusts)
                _state.value = _state.value.copy(
                    mangaList = cacheResult.data.illusts,
                    isLoadingManga = cacheResult.shouldFetch(forceRefresh)
                )
            }

            if (!cacheResult.shouldFetch(forceRefresh)) return@launch

            try {
                val response = PixivClient.pixivApi.getUserIllusts(userId, type = "manga")
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    mangaList = response.illusts,
                    isLoadingManga = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingManga = false)
            }
        }
    }

    private fun loadNovels(userId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingNovels = true)

            val cacheConfig = CacheConfig(
                path = "/v1/user/novels",
                queryParams = mapOf("user_id" to userId.toString(), "filter" to "for_ios")
            )

            val cacheResult = cacheConfig.loadFromCache(NovelResponse::class.java)
            if (cacheResult != null) {
                storeNovels(cacheResult.data.novels)
                _state.value = _state.value.copy(
                    novels = cacheResult.data.novels,
                    isLoadingNovels = cacheResult.shouldFetch(false)
                )
            }

            if (!cacheResult.shouldFetch(false)) return@launch

            try {
                val response = PixivClient.pixivApi.getUserNovels(userId)
                storeNovels(response.novels)
                _state.value = _state.value.copy(
                    novels = response.novels,
                    isLoadingNovels = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingNovels = false)
            }
        }
    }

    private fun loadBookmarkedIllusts(userId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingBookmarkedIllusts = true)

            val cacheConfig = CacheConfig(
                path = "/v1/user/bookmarks/illust",
                queryParams = mapOf(
                    "user_id" to userId.toString(),
                    "restrict" to "public",
                    "filter" to "for_ios"
                )
            )

            val cacheResult = cacheConfig.loadFromCache(IllustResponse::class.java)
            if (cacheResult != null) {
                storeIllusts(cacheResult.data.illusts)
                _state.value = _state.value.copy(
                    bookmarkedIllusts = cacheResult.data.illusts,
                    isLoadingBookmarkedIllusts = cacheResult.shouldFetch(false)
                )
            }

            if (!cacheResult.shouldFetch(false)) return@launch

            try {
                val response = PixivClient.pixivApi.getUserBookmarks(userId)
                storeIllusts(response.illusts)
                _state.value = _state.value.copy(
                    bookmarkedIllusts = response.illusts,
                    isLoadingBookmarkedIllusts = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingBookmarkedIllusts = false)
            }
        }
    }

    private fun loadBookmarkedNovels(userId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingBookmarkedNovels = true)

            val cacheConfig = CacheConfig(
                path = "/v1/user/bookmarks/novel",
                queryParams = mapOf(
                    "user_id" to userId.toString(),
                    "restrict" to "public",
                    "filter" to "for_ios"
                )
            )

            val cacheResult = cacheConfig.loadFromCache(NovelResponse::class.java)
            if (cacheResult != null) {
                storeNovels(cacheResult.data.novels)
                _state.value = _state.value.copy(
                    bookmarkedNovels = cacheResult.data.novels,
                    isLoadingBookmarkedNovels = cacheResult.shouldFetch(false)
                )
            }

            if (!cacheResult.shouldFetch(false)) return@launch

            try {
                val response = PixivClient.pixivApi.getUserBookmarkNovels(userId)
                storeNovels(response.novels)
                _state.value = _state.value.copy(
                    bookmarkedNovels = response.novels,
                    isLoadingBookmarkedNovels = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingBookmarkedNovels = false)
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
    fun toggleFollow(restrict: String = "public") {
        val user = _state.value.user ?: return
        val isCurrentlyFollowed = user.is_followed == true

        viewModelScope.launch {
            _state.value = _state.value.copy(isFollowing = true)

            try {
                if (isCurrentlyFollowed) {
                    PixivClient.pixivApi.unfollowUser(user.id)
                } else {
                    PixivClient.pixivApi.followUser(user.id, restrict)
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
        Log.d(TAG, "storeIllusts: storing ${illusts.size} illusts")
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
        Log.d(TAG, "storeIllusts: done, ObjectStore size = ${ObjectStore.size()}")
    }

    companion object {
        private const val TAG = "UserProfileVM"
    }

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
}
