package ceui.lisa.jcstaff.core

import ceui.lisa.jcstaff.cache.ApiCacheManager
import ceui.lisa.jcstaff.network.PagedResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    val isEmpty: Boolean get() = items.isEmpty()
    val hasError: Boolean get() = error != null

    // 兼容属性，方便各类型使用
    val illusts: List<T> get() = items
    val novels: List<T> get() = items
    val users: List<T> get() = items
}

/**
 * 通用非分页数据状态
 */
data class SimpleState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

private val CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(15)

/**
 * 缓存配置
 */
data class CacheConfig(
    val path: String,
    val queryParams: Map<String, String> = emptyMap()
) {
    fun buildUrl(): String {
        val baseUrl = "https://app-api.pixiv.net$path"
        if (queryParams.isEmpty()) return baseUrl
        val queryString = queryParams.entries.joinToString("&") { (k, v) -> "$k=$v" }
        return "$baseUrl?$queryString"
    }

    fun buildCacheKey(): String = ApiCacheManager.buildCacheKey("GET", buildUrl())

    /**
     * 从缓存加载数据
     */
    suspend fun <T> loadFromCache(clazz: Class<T>): CacheResult<T>? = withContext(Dispatchers.Default) {
        val cacheEntry = ApiCacheManager.getStale(buildCacheKey()) ?: return@withContext null
        try {
            val json = String(cacheEntry.responseBody, Charsets.UTF_8)
            val response = com.google.gson.Gson().fromJson(json, clazz)
            CacheResult(response, cacheEntry.timestamp)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 缓存加载结果
 */
data class CacheResult<T>(
    val data: T,
    val timestamp: Long
) {
    val isFresh: Boolean get() = System.currentTimeMillis() - timestamp <= CACHE_DURATION_MS

    fun shouldFetch(forceRefresh: Boolean): Boolean = forceRefresh || !isFresh
}

/** 判断是否需要发网络请求 */
fun CacheResult<*>?.shouldFetch(forceRefresh: Boolean): Boolean {
    return this?.shouldFetch(forceRefresh) ?: true
}

/**
 * 通用分页数据加载器
 *
 * 使用组合而非继承，ViewModel 持有此 loader 实例
 *
 * @param T 列表项类型
 * @param R API 响应类型，必须实现 PagedResponse<T>
 * @param cacheConfigProvider 动态缓存配置提供者，支持根据当前状态构建不同的缓存 key
 */
class PagedDataLoader<T, R : PagedResponse<T>>(
    private val cacheConfigProvider: () -> CacheConfig?,
    private val responseClass: Class<R>,
    private val loadFirstPage: suspend () -> R,
    private val onItemsLoaded: (List<T>) -> Unit = {}
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    /** 便捷构造器：固定 cacheConfig */
    constructor(
        cacheConfig: CacheConfig?,
        responseClass: Class<R>,
        loadFirstPage: suspend () -> R,
        onItemsLoaded: (List<T>) -> Unit = {}
    ) : this({ cacheConfig }, responseClass, loadFirstPage, onItemsLoaded)

    /** 重置状态（用于切换筛选条件等场景） */
    fun reset() {
        _state.value = PagedState()
    }

    /** 更新 items 列表（用于新增/删除场景） */
    fun updateItems(transform: (List<T>) -> List<T>) {
        _state.value = _state.value.copy(items = transform(_state.value.items))
    }

    /**
     * 加载数据
     * @param forceRefresh 是否强制刷新（下拉刷新/点击重试时为 true）
     */
    suspend fun load(forceRefresh: Boolean = false) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        // Step 1: 从缓存加载（每次调用 cacheConfigProvider 获取最新配置）
        val cacheResult = cacheConfigProvider()?.loadFromCache(responseClass)

        if (cacheResult != null) {
            onItemsLoaded(cacheResult.data.displayList)
            _state.value = _state.value.copy(
                items = cacheResult.data.displayList,
                isLoading = cacheResult.shouldFetch(forceRefresh),
                nextUrl = cacheResult.data.nextUrl
            )
        }

        // Step 2: 判断是否需要发网络请求
        if (!cacheResult.shouldFetch(forceRefresh)) {
            return
        }

        // Step 3: 从网络加载
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
            _state.value = _state.value.copy(
                isLoading = false,
                error = if (_state.value.items.isEmpty()) e.message ?: "加载失败" else null
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
        load(forceRefresh = true)
    }
}
