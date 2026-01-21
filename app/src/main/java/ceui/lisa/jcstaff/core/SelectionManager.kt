package ceui.lisa.jcstaff.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ceui.lisa.jcstaff.network.Illust

/**
 * 管理批量选择状态
 * 类似 Google 相册的选择模式
 */
@Stable
class SelectionManager {
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedItems by mutableStateOf<Set<Long>>(emptySet())
        private set

    private var illustCache = mutableMapOf<Long, Illust>()

    val selectedCount: Int
        get() = selectedItems.size

    fun isSelected(illustId: Long): Boolean = selectedItems.contains(illustId)

    /**
     * 长按触发选择模式并选中当前项
     */
    fun onLongPress(illust: Illust) {
        if (!isSelectionMode) {
            isSelectionMode = true
        }
        illustCache[illust.id] = illust
        selectedItems = selectedItems + illust.id
    }

    /**
     * 点击切换选中状态（仅在选择模式下）
     */
    fun toggleSelection(illust: Illust) {
        if (!isSelectionMode) return

        if (selectedItems.contains(illust.id)) {
            selectedItems = selectedItems - illust.id
            illustCache.remove(illust.id)
            // 如果没有选中项，退出选择模式
            if (selectedItems.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            illustCache[illust.id] = illust
            selectedItems = selectedItems + illust.id
        }
    }

    /**
     * 获取选中的 Illust 列表
     */
    fun getSelectedIllusts(): List<Illust> {
        return selectedItems.mapNotNull { illustCache[it] }
    }

    /**
     * 全选
     */
    fun selectAll(illusts: List<Illust>) {
        isSelectionMode = true
        illusts.forEach { illustCache[it.id] = it }
        selectedItems = illusts.map { it.id }.toSet()
    }

    /**
     * 清除选择并退出选择模式
     */
    fun clearSelection() {
        selectedItems = emptySet()
        illustCache.clear()
        isSelectionMode = false
    }
}

@Composable
fun rememberSelectionManager(): SelectionManager {
    return remember { SelectionManager() }
}
