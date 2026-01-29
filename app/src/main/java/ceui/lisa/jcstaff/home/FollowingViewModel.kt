package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.IllustResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 保留类型别名，方便 UI 层使用
typealias FollowingUiState = PagedState<Illust>

class FollowingViewModel : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = CacheConfig(
            path = "/v2/illust/follow",
            queryParams = mapOf("restrict" to "public")
        ),
        responseClass = IllustResponse::class.java,
        loadFirstPage = { PixivClient.pixivApi.getFollowingIllusts() },
        onItemsLoaded = { illusts -> storeIllusts(illusts) }
    )

    val state: StateFlow<FollowingUiState> = loader.state

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
}
