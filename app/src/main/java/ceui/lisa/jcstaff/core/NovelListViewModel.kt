package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NovelListViewModel(
    loadFirstPage: suspend () -> NovelResponse,
    cacheConfig: CacheConfig? = null
) : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = cacheConfig,
        responseClass = NovelResponse::class.java,
        loadFirstPage = loadFirstPage,
        onItemsLoaded = ::storeNovels
    )

    val state: StateFlow<PagedState<Novel>> = loader.state

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

    private fun storeNovels(novels: List<Novel>) {
        novels.forEach { novel ->
            ObjectStore.put(novel)
            novel.user?.let { user -> ObjectStore.put(user) }
        }
    }

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
