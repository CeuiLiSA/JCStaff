package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.NovelResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private var isBound = false

    fun bind(loader: NovelLoader) {
        if (isBound) return
        this.loader = loader
        this.isBound = true
        load()
    }

    fun load() {
        val currentLoader = loader ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
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
                    error = e.message ?: "加载失败"
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

    fun refresh() = load()
}
