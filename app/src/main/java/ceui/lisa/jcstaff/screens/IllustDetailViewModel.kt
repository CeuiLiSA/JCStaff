package ceui.lisa.jcstaff.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 插画详情页状态
 */
data class IllustDetailState(
    val illust: Illust? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isBookmarked: Boolean = false
)

/**
 * 插画详情页 ViewModel
 */
class IllustDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(IllustDetailState())
    val state: StateFlow<IllustDetailState> = _state.asStateFlow()

    private var currentIllustId: Long? = null

    /**
     * 绑定插画 ID 并加载数据
     */
    fun bind(illustId: Long) {
        if (currentIllustId == illustId) return
        currentIllustId = illustId

        // 先尝试从缓存获取
        val cachedIllust = ObjectStore.peek<Illust>(StoreKey(illustId, StoreType.ILLUST))

        _state.update {
            it.copy(
                illust = cachedIllust,
                isLoading = cachedIllust == null,
                isBookmarked = cachedIllust?.is_bookmarked ?: false,
                error = null
            )
        }

        // 观察 ObjectStore 中的更新
        observeIllust(illustId)

        // 如果没有缓存，从 API 加载
        if (cachedIllust == null) {
            loadIllustDetail(illustId)
        } else {
            // 有缓存时记录浏览历史
            recordBrowseHistory(cachedIllust)
        }
    }

    /**
     * 观察 ObjectStore 中的插画更新
     */
    private fun observeIllust(illustId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            ObjectStore.get<Illust>(StoreKey(illustId, StoreType.ILLUST))?.collect { illust ->
                _state.update {
                    it.copy(
                        illust = illust,
                        isBookmarked = illust.is_bookmarked ?: false
                    )
                }
            }
        }
    }

    /**
     * 从 API 加载插画详情
     */
    private fun loadIllustDetail(illustId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = PixivClient.pixivApi.getIllustDetail(illustId)
                response.illust?.let { fetchedIllust ->
                    // 存入 ObjectStore
                    ObjectStore.put(fetchedIllust)
                    fetchedIllust.user?.let { user -> ObjectStore.put(user) }

                    _state.update {
                        it.copy(
                            illust = fetchedIllust,
                            isLoading = false,
                            isBookmarked = fetchedIllust.is_bookmarked ?: false,
                            error = null
                        )
                    }

                    // 记录浏览历史
                    recordBrowseHistory(fetchedIllust)
                } ?: run {
                    _state.update {
                        it.copy(isLoading = false, error = "加载失败")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    /**
     * 记录浏览历史
     */
    private fun recordBrowseHistory(illust: Illust) {
        BrowseHistoryRepository.recordIllust(illust)
    }

    /**
     * 更新收藏状态
     */
    fun updateBookmarkState(isBookmarked: Boolean, updatedIllust: Illust) {
        _state.update {
            it.copy(
                illust = updatedIllust,
                isBookmarked = isBookmarked
            )
        }
    }

    /**
     * 获取作者关注状态
     */
    fun getUserFollowState(userId: Long): StateFlow<User?>? {
        return ObjectStore.get<User>(StoreKey(userId, StoreType.USER))
    }

    /**
     * 获取第一张原图 URL
     */
    fun getFirstOriginalUrl(): String? {
        val illust = _state.value.illust ?: return null
        return if (illust.page_count == 1) {
            illust.meta_single_page?.original_image_url
        } else {
            illust.meta_pages?.firstOrNull()?.image_urls?.original
        }
    }
}
