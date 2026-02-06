package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IllustListViewModel(
    loadFirstPage: suspend () -> IllustResponse,
    cacheConfig: CacheConfig? = null
) : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = cacheConfig,
        responseClass = IllustResponse::class.java,
        loadFirstPage = loadFirstPage,
        onItemsLoaded = ::storeIllusts
    )

    val state: StateFlow<PagedState<Illust>> = loader.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
    }

    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user -> ObjectStore.put(user) }
        }
    }

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
