package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse

class IllustListViewModel(
    loadFirstPage: suspend () -> IllustResponse,
    cacheConfig: CacheConfig? = null
) : PagedViewModel<Illust, IllustResponse>(
    cacheConfig = cacheConfig,
    responseClass = IllustResponse::class.java,
    loadFirstPage = loadFirstPage,
    onItemsLoaded = { illusts ->
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }
) {
    companion object {
        fun factory(
            loadFirstPage: suspend () -> IllustResponse,
            cacheConfig: CacheConfig? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IllustListViewModel(loadFirstPage, cacheConfig) as T
            }
        }
    }
}
