package ceui.lisa.jcstaff.core

import ceui.lisa.jcstaff.cache.ApiCacheManager
import ceui.lisa.jcstaff.network.PagedResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

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
 * 缓存策略 (stale-while-revalidate):
 * 1. 首先检查并展示缓存（无论是否过期）
 * 2. 如果是初次加载且缓存未过期，不发网络请求
 * 3. 如果是强制刷新、缓存过期或不存在，发网络请求
 * 4. 网络请求成功则更新数据
 * 5. 网络请求失败且无缓存，才显示错误
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

    companion object {
        private val CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(15)
    }

    /**
     * 加载数据
     * @param forceRefresh 是否强制刷新（下拉刷新/点击重试时为 true，始终发网络请求）
     */
    suspend fun load(forceRefresh: Boolean = false) {
        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        var hasFreshCache = false

        // Step 1: 立即展示缓存（如果有）
        if (cacheConfig != null) {
            val cacheResult = loadFromCache()
            if (cacheResult != null) {
                val (cached, timestamp) = cacheResult
                hasFreshCache = !isExpired(timestamp)

                onItemsLoaded(cached.displayList)
                _state.value = _state.value.copy(
                    items = cached.displayList,
                    // 如果缓存未过期且非强制刷新，不需要显示 loading
                    isLoading = if (forceRefresh) true else !hasFreshCache,
                    nextUrl = cached.nextUrl
                )
            }
        }

        // Step 2: 如果缓存未过期且非强制刷新，不需要发网络请求
        if (hasFreshCache && !forceRefresh) {
            return
        }

        // Step 3: 强制刷新、缓存过期或不存在，从网络加载
        try {
            val response = loadFirstPage()
            onItemsLoaded(response.displayList)
            _state.value = _state.value.copy(
                items = response.displayList,
                isLoading = false,
                nextUrl = response.nextUrl,
                error = null
            )
        } catch (e: Exception) {
            // 网络请求失败
            _state.value = _state.value.copy(
                isLoading = false,
                // 只有当没有缓存数据时才显示错误
                error = if (_state.value.items.isEmpty()) {
                    e.message ?: "加载失败"
                } else null
            )
        }
    }

    /**
     * 从缓存加载数据
     * @return Pair<响应数据, 缓存时间戳> 或 null
     */
    private suspend fun loadFromCache(): Pair<R, Long>? {
        if (cacheConfig == null) return null

        val url = buildCacheUrl()
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)
        val cacheEntry = ApiCacheManager.getStale(cacheKey) ?: return null

        return try {
            val json = String(cacheEntry.responseBody, Charsets.UTF_8)
            val response = com.google.gson.Gson().fromJson(json, responseClass)
            Pair(response, cacheEntry.timestamp)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildCacheUrl(): String {
        val config = cacheConfig ?: return ""
        val baseUrl = "https://app-api.pixiv.net" + config.path
        if (config.queryParams.isEmpty()) return baseUrl

        val queryString = config.queryParams.entries.joinToString("&") { (key, value) ->
            "$key=$value"
        }
        return "$baseUrl?$queryString"
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS
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

    /**
     * 强制刷新，始终发网络请求
     */
    suspend fun refresh() {
        load(forceRefresh = true)
    }
}
