package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse

class NovelListViewModel(
    loadFirstPage: suspend () -> NovelResponse,
    cacheConfig: CacheConfig? = null
) : PagedViewModel<Novel, NovelResponse>(
    cacheConfig = cacheConfig,
    responseClass = NovelResponse::class.java,
    loadFirstPage = loadFirstPage,
    onItemsLoaded = { novels ->
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }
) {
    companion object {
        fun factory(
            loadFirstPage: suspend () -> NovelResponse,
            cacheConfig: CacheConfig? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NovelListViewModel(loadFirstPage, cacheConfig) as T
            }
        }
    }
}
