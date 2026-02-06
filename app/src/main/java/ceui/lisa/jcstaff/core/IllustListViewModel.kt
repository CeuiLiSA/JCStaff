package ceui.lisa.jcstaff.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 插画列表加载器（支持分页）
 * 返回 IllustResponse 以获取 next_url
 */
fun interface IllustLoader {
    suspend fun load(): IllustResponse
}

private const val TAG = "IllustListVM"

/**
 * 插画列表状态
 */
data class IllustListState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val isEmpty: Boolean get() = illusts.isEmpty()
    val hasError: Boolean get() = error != null
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

/**
 * 通用插画列表 ViewModel
 *
 * 使用方式：
 * ```
 * // 收藏列表
 * val bookmarksLoader = IllustLoader {
 *     PixivClient.pixivApi.getUserBookmarks(userId)
 * }
 * viewModel.bind(bookmarksLoader)
 *
 * // 相关作品
 * val relatedLoader = IllustLoader {
 *     PixivClient.pixivApi.getRelatedIllusts(illustId)
 * }
 * viewModel.bind(relatedLoader)
 * ```
 */
class IllustListViewModel : ViewModel() {

    private val _state = MutableStateFlow(IllustListState())
    val state: StateFlow<IllustListState> = _state.asStateFlow()

    private var loader: IllustLoader? = null
    private var cacheConfig: CacheConfig? = null
    private var isBound = false

    /**
     * 绑定加载器，首次绑定时自动加载
     * 同一个 ViewModel 实例只能绑定一次
     * @param loader 加载器
     * @param cacheConfig 可选的缓存配置，传入后会先检查缓存
     */
    fun bind(loader: IllustLoader, cacheConfig: CacheConfig? = null) {
        if (isBound) return
        this.loader = loader
        this.cacheConfig = cacheConfig
        this.isBound = true
        load(forceRefresh = false)
    }

    /**
     * 加载数据（首次加载或刷新）
     */
    fun load(forceRefresh: Boolean = true) {
        val currentLoader = loader ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 先尝试从缓存加载
            val config = cacheConfig
            if (config != null) {
                val cacheResult = config.loadFromCache(IllustResponse::class.java)
                if (cacheResult != null) {
                    Log.d(TAG, "load: cache hit, ${cacheResult.data.illusts.size} illusts")
                    storeIllusts(cacheResult.data.illusts)
                    _state.value = _state.value.copy(
                        illusts = cacheResult.data.illusts,
                        isLoading = cacheResult.shouldFetch(forceRefresh),
                        nextUrl = cacheResult.data.next_url
                    )
                    // 如果不需要刷新，直接返回
                    if (!cacheResult.shouldFetch(forceRefresh)) {
                        Log.d(TAG, "load: cache fresh, skip network")
                        return@launch
                    }
                }
            }

            try {
                Log.d(TAG, "load: fetching from network")
                val response = currentLoader.load()

                // 存入 ObjectStore
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = response.illusts,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.illusts.isEmpty()) e.message ?: "加载失败" else null
                )
            }
        }
    }

    /**
     * 加载更多（分页加载）
     */
    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            try {
                val response = PixivClient.getNextPage(nextUrl, IllustResponse::class.java)

                // 存入 ObjectStore
                storeIllusts(response.illusts)

                _state.value = _state.value.copy(
                    illusts = _state.value.illusts + response.illusts,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

    /**
     * 刷新（重新加载，强制刷新）
     */
    fun refresh() = load(forceRefresh = true)
}
