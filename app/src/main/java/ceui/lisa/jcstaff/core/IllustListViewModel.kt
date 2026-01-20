package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 插画列表加载器
 * 外部传入不同的加载逻辑，ViewModel 负责统一管理加载状态
 */
fun interface IllustLoader {
    suspend fun load(): List<Illust>
}

/**
 * 插画列表状态
 */
data class IllustListState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = illusts.isEmpty()
    val hasError: Boolean get() = error != null
}

/**
 * 通用插画列表 ViewModel
 *
 * 使用方式：
 * ```
 * // 收藏列表
 * val bookmarksLoader = IllustLoader {
 *     PixivClient.pixivApi.getUserBookmarks(userId).illusts
 * }
 * viewModel.bind(bookmarksLoader)
 *
 * // 推荐列表
 * val recommendedLoader = IllustLoader {
 *     PixivClient.pixivApi.getRecommendedIllusts().displayList
 * }
 * viewModel.bind(recommendedLoader)
 *
 * // 相关作品
 * val relatedLoader = IllustLoader {
 *     PixivClient.pixivApi.getRelatedIllusts(illustId).illusts
 * }
 * viewModel.bind(relatedLoader)
 * ```
 */
class IllustListViewModel : ViewModel() {

    private val _state = MutableStateFlow(IllustListState())
    val state: StateFlow<IllustListState> = _state.asStateFlow()

    private var loader: IllustLoader? = null
    private var isBound = false

    /**
     * 绑定加载器，首次绑定时自动加载
     * 同一个 ViewModel 实例只能绑定一次
     */
    fun bind(loader: IllustLoader) {
        if (isBound) return
        this.loader = loader
        this.isBound = true
        load()
    }

    /**
     * 加载数据
     */
    fun load() {
        val currentLoader = loader ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val illusts = currentLoader.load()

                // 存入 ObjectStore
                illusts.forEach { illust ->
                    ObjectStore.put(illust)
                    illust.user?.let { user -> ObjectStore.put(user) }
                }

                _state.value = _state.value.copy(
                    illusts = illusts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 刷新（重新加载）
     */
    fun refresh() = load()
}
