package ceui.lisa.jcstaff.core

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

/**
 * 滚动位置数据
 */
data class ScrollPosition(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0
)

/**
 * 全局滚动位置存储
 * 用于在页面导航时保持列表滚动位置
 */
object ScrollPositionStore {
    private val positions = mutableMapOf<String, ScrollPosition>()

    fun save(key: String, index: Int, offset: Int) {
        positions[key] = ScrollPosition(index, offset)
    }

    fun get(key: String): ScrollPosition? = positions[key]

    fun remove(key: String) {
        positions.remove(key)
    }

    fun clear() {
        positions.clear()
    }
}

/**
 * 持久化的 LazyStaggeredGridState
 * 自动保存和恢复滚动位置
 *
 * @param key 唯一标识符，用于区分不同的列表
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberPersistentLazyStaggeredGridState(key: String): LazyStaggeredGridState {
    val savedPosition = remember(key) { ScrollPositionStore.get(key) }

    val state = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = savedPosition?.firstVisibleItemIndex ?: 0,
        initialFirstVisibleItemScrollOffset = savedPosition?.firstVisibleItemScrollOffset ?: 0
    )

    // 监听滚动位置变化并保存
    LaunchedEffect(key, state) {
        snapshotFlow {
            state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
        }
            .debounce(100) // 防抖，避免频繁保存
            .collectLatest { (index, offset) ->
                ScrollPositionStore.save(key, index, offset)
            }
    }

    return state
}