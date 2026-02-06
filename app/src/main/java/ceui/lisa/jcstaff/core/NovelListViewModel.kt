package ceui.lisa.jcstaff.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "NovelListVM"

/**
 * 小说列表加载器（支持分页）
 * 返回 NovelResponse 以获取 next_url
 */
fun interface NovelLoader {
    suspend fun load(): NovelResponse
}

/**
 * 小说列表状态
 */
data class NovelListState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val isEmpty: Boolean get() = novels.isEmpty()
    val hasError: Boolean get() = error != null
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

/**
 * 通用小说列表 ViewModel
 */
class NovelListViewModel : ViewModel() {

    private val _state = MutableStateFlow(NovelListState())
    val state: StateFlow<NovelListState> = _state.asStateFlow()

    private var loader: NovelLoader? = null
    private var cacheConfig: CacheConfig? = null
    private var isBound = false

    fun bind(loader: NovelLoader, cacheConfig: CacheConfig? = null) {
        if (isBound) return
        this.loader = loader
        this.cacheConfig = cacheConfig
        this.isBound = true
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean = true) {
        val currentLoader = loader ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 先尝试从缓存加载
            val config = cacheConfig
            if (config != null) {
                val cacheResult = config.loadFromCache(NovelResponse::class.java)
                if (cacheResult != null) {
                    Log.d(TAG, "load: cache hit, ${cacheResult.data.novels.size} novels")
                    storeNovels(cacheResult.data.novels)
                    _state.value = _state.value.copy(
                        novels = cacheResult.data.novels,
                        isLoading = cacheResult.shouldFetch(forceRefresh),
                        nextUrl = cacheResult.data.next_url
                    )
                    if (!cacheResult.shouldFetch(forceRefresh)) {
                        Log.d(TAG, "load: cache fresh, skip network")
                        return@launch
                    }
                }
            }

            try {
                Log.d(TAG, "load: fetching from network")
                val response = currentLoader.load()
                storeNovels(response.novels)

                _state.value = _state.value.copy(
                    novels = response.novels,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.novels.isEmpty()) e.message ?: "加载失败" else null
                )
            }
        }
    }

    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            try {
                val response = PixivClient.getNextPage(nextUrl, NovelResponse::class.java)
                storeNovels(response.novels)

                _state.value = _state.value.copy(
                    novels = _state.value.novels + response.novels,
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

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }

    fun refresh() = load(forceRefresh = true)
}
