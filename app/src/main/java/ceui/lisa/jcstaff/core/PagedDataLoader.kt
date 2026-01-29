package ceui.lisa.jcstaff.core

import ceui.lisa.jcstaff.network.PagedResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通用分页数据状态
 */
data class PagedState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore

    // 兼容属性，方便各类型使用
    val illusts: List<T> get() = items
    val novels: List<T> get() = items
    val users: List<T> get() = items
}

/**
 * 缓存配置
 */
data class CacheConfig(
    val path: String,
    val queryParams: Map<String, String> = emptyMap()
)

/**
 * 通用分页数据加载器
 *
 * 使用组合而非继承，ViewModel 持有此 loader 实例
 *
 * @param T 列表项类型
 * @param R API 响应类型，必须实现 PagedResponse<T>
 */
class PagedDataLoader<T, R : PagedResponse<T>>(
    private val cacheConfig: CacheConfig?,
    private val responseClass: Class<R>,
    private val loadFirstPage: suspend () -> R,
    private val onItemsLoaded: (List<T>) -> Unit = {}
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    suspend fun load() {
        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        // 尝试从缓存加载
        if (cacheConfig != null) {
            val cached = PixivClient.getFromStaleCache(
                path = cacheConfig.path,
                queryParams = cacheConfig.queryParams,
                clazz = responseClass
            )
            if (cached != null) {
                onItemsLoaded(cached.displayList)
                _state.value = _state.value.copy(
                    items = cached.displayList,
                    isLoading = false,
                    nextUrl = cached.nextUrl
                )
            }
        }

        // 从网络加载
        try {
            val response = loadFirstPage()
            onItemsLoaded(response.displayList)
            _state.value = _state.value.copy(
                items = response.displayList,
                isLoading = false,
                nextUrl = response.nextUrl
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = if (_state.value.items.isEmpty()) {
                    e.message ?: "加载失败"
                } else null
            )
        }
    }

    suspend fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        _state.value = _state.value.copy(isLoadingMore = true)
        try {
            val response = PixivClient.getNextPage(nextUrl, responseClass)
            onItemsLoaded(response.displayList)
            _state.value = _state.value.copy(
                items = _state.value.items + response.displayList,
                isLoadingMore = false,
                nextUrl = response.nextUrl
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoadingMore = false,
                error = e.message ?: "加载更多失败"
            )
        }
    }

    suspend fun refresh() {
        load()
    }
}
