package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.ApiCacheManager
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.shouldFetch
import ceui.lisa.jcstaff.network.HomeAllReq
import ceui.lisa.jcstaff.network.HomeAllResponse
import ceui.lisa.jcstaff.network.HomeContent
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.NextPageSpec
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivClient
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI 状态
 */
data class HomeAllUiState(
    val items: List<HomeAllUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextParams: NextPageSpec? = null
) {
    val canLoadMore: Boolean get() = nextParams != null && !isLoadingMore
}

/**
 * UI item sealed interface — each variant maps to a layout type in the feed
 */
sealed interface HomeAllUiItem {
    data class IllustItem(
        val illust: Illust,
        val pickup: ceui.lisa.jcstaff.network.Pickup? = null
    ) : HomeAllUiItem

    data class NovelItem(
        val novel: Novel
    ) : HomeAllUiItem

    data class TagsCarouselItem(
        val tag: String?,
        val illusts: List<Illust>,
        val index: Int
    ) : HomeAllUiItem

    data class SeparatorItem(
        val index: Int
    ) : HomeAllUiItem
}

class HomeAllViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeAllUiState())
    val state: StateFlow<HomeAllUiState> = _state.asStateFlow()

    private val gson = Gson()

    /** Raw (unfiltered) items for re-filtering when block rules change */
    private val rawItems = mutableListOf<HomeAllUiItem>()

    private val cacheConfig = CacheConfig(path = "/v1/home/all")

    init {
        load(forceRefresh = false)
        ContentFilterManager.onFilterChanged(viewModelScope) {
            _state.value = _state.value.copy(
                items = filterItems(rawItems)
            )
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 1. 从缓存加载
            val cacheResult = cacheConfig.loadFromCache(HomeAllResponse::class.java)
            if (cacheResult != null) {
                resetCounters()
                val processed = processContents(cacheResult.data.contents)
                rawItems.clear()
                rawItems.addAll(processed)
                _state.value = _state.value.copy(
                    items = filterItems(rawItems),
                    isLoading = cacheResult.shouldFetch(forceRefresh),
                    nextParams = cacheResult.data.nextParams
                )
            }

            // 2. 判断是否需要发网络请求
            if (!cacheResult.shouldFetch(forceRefresh)) {
                return@launch
            }

            // 3. 从网络加载
            try {
                val response = PixivClient.pixivApi.getHomeAll(HomeAllReq())
                saveToCache(response)
                resetCounters()
                val processed = processContents(response.contents)
                rawItems.clear()
                rawItems.addAll(processed)
                _state.value = _state.value.copy(
                    items = filterItems(rawItems),
                    isLoading = false,
                    nextParams = response.nextParams
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.items.isEmpty()) e.message ?: "加载失败" else null
                )
            }
        }
    }

    fun loadMore() {
        val params = _state.value.nextParams ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            try {
                val response = PixivClient.pixivApi.getHomeAll(
                    HomeAllReq(next_params = params)
                )
                val processed = processContents(response.contents)
                rawItems.addAll(processed)
                _state.value = _state.value.copy(
                    items = _state.value.items + filterItems(processed),
                    isLoadingMore = false,
                    nextParams = response.nextParams
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    // ===== Cache =====

    private suspend fun saveToCache(response: HomeAllResponse) {
        try {
            val json = gson.toJson(response)
            ApiCacheManager.put(
                key = cacheConfig.buildCacheKey(),
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = 200,
                httpMessage = "OK"
            )
        } catch (_: Exception) {
            // 缓存写入失败不影响正常使用
        }
    }

    // ===== Bookmark =====

    /**
     * Toggle illust bookmark state optimistically
     */
    fun toggleBookmark(illust: Illust) {
        val wasBookmarked = illust.is_bookmarked == true
        // Optimistic update in ObjectStore
        ObjectStore.updateTyped<Illust>(illust.storeKey) {
            it.copy(is_bookmarked = !wasBookmarked)
        }
        // Update local items
        updateIllustInItems(illust.id) { it.copy(is_bookmarked = !wasBookmarked) }

        viewModelScope.launch {
            try {
                if (wasBookmarked) {
                    PixivClient.pixivApi.deleteBookmark(illust.id)
                } else {
                    PixivClient.pixivApi.addBookmark(illust.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Revert on failure
                ObjectStore.updateTyped<Illust>(illust.storeKey) {
                    it.copy(is_bookmarked = wasBookmarked)
                }
                updateIllustInItems(illust.id) { it.copy(is_bookmarked = wasBookmarked) }
            }
        }
    }

    private fun updateIllustInItems(illustId: Long, transform: (Illust) -> Illust) {
        val updated = _state.value.items.map { item ->
            when (item) {
                is HomeAllUiItem.IllustItem -> {
                    if (item.illust.id == illustId) item.copy(illust = transform(item.illust)) else item
                }
                is HomeAllUiItem.TagsCarouselItem -> {
                    val updatedIllusts = item.illusts.map {
                        if (it.id == illustId) transform(it) else it
                    }
                    item.copy(illusts = updatedIllusts)
                }
                else -> item
            }
        }
        _state.value = _state.value.copy(items = updated)
        // Also update raw items
        rawItems.forEachIndexed { index, item ->
            when (item) {
                is HomeAllUiItem.IllustItem -> {
                    if (item.illust.id == illustId) rawItems[index] = item.copy(illust = transform(item.illust))
                }
                is HomeAllUiItem.TagsCarouselItem -> {
                    val updatedIllusts = item.illusts.map {
                        if (it.id == illustId) transform(it) else it
                    }
                    rawItems[index] = item.copy(illusts = updatedIllusts)
                }
                else -> {}
            }
        }
    }

    // ===== Processing =====

    private var separatorCount = 0
    private var carouselCount = 0

    private fun resetCounters() {
        separatorCount = 0
        carouselCount = 0
    }

    /**
     * Convert raw API contents to typed UI items.
     * app_model is a LinkedTreeMap that we roundtrip through Gson to get typed Illust/Novel.
     */
    private fun processContents(contents: List<HomeContent>): List<HomeAllUiItem> {
        return contents.mapNotNull { content ->
            when (content.kind) {
                "illust", "manga" -> {
                    val appModel = content.thumbnails?.firstOrNull()?.app_model
                    val illust = deserializeAppModel<Illust>(appModel) ?: return@mapNotNull null
                    storeIllust(illust)
                    HomeAllUiItem.IllustItem(illust = illust, pickup = content.pickup)
                }
                "novel" -> {
                    val appModel = content.thumbnails?.firstOrNull()?.app_model
                    val novel = deserializeAppModel<Novel>(appModel) ?: return@mapNotNull null
                    storeNovel(novel)
                    HomeAllUiItem.NovelItem(novel = novel)
                }
                "tags_carousel" -> {
                    val illusts = content.thumbnails?.mapNotNull { thumb ->
                        deserializeAppModel<Illust>(thumb.app_model)?.also { storeIllust(it) }
                    } ?: emptyList()
                    if (illusts.isEmpty()) return@mapNotNull null
                    HomeAllUiItem.TagsCarouselItem(
                        tag = null,
                        illusts = illusts,
                        index = carouselCount++
                    )
                }
                "separator" -> {
                    HomeAllUiItem.SeparatorItem(index = separatorCount++)
                }
                else -> null
            }
        }
    }

    private inline fun <reified T> deserializeAppModel(appModel: Any?): T? {
        if (appModel == null) return null
        return try {
            val json = gson.toJson(appModel)
            gson.fromJson(json, T::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun storeIllust(illust: Illust) {
        ObjectStore.put(illust)
        illust.user?.let { ObjectStore.put(it) }
    }

    private fun storeNovel(novel: Novel) {
        ObjectStore.put(novel)
        novel.user?.let { ObjectStore.put(it) }
    }

    private fun filterItems(items: List<HomeAllUiItem>): List<HomeAllUiItem> {
        return items.filter { item ->
            when (item) {
                is HomeAllUiItem.IllustItem -> ContentFilterManager.shouldShow(item.illust)
                is HomeAllUiItem.NovelItem -> ContentFilterManager.shouldShow(item.novel)
                is HomeAllUiItem.TagsCarouselItem -> true
                is HomeAllUiItem.SeparatorItem -> true
            }
        }
    }
}
