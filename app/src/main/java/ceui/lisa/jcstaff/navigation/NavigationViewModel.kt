package ceui.lisa.jcstaff.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.components.appswitcher.ScreenshotStore
import kotlinx.coroutines.launch

enum class NavigationDirection { FORWARD, BACKWARD }

data class AppSwitcherState(
    val isVisible: Boolean = false,
    val selectedIndex: Int = -1
)

class NavigationViewModel : ViewModel() {

    val backStack: SnapshotStateList<NavRoute> = mutableStateListOf()

    val currentRoute: NavRoute?
        get() = backStack.lastOrNull()

    val canGoBack: Boolean
        get() = backStack.size > 1

    // Navigation direction for transition animations
    private val _navigationDirection = mutableStateOf(NavigationDirection.FORWARD)
    val navigationDirection: State<NavigationDirection> = _navigationDirection

    val previousRouteKey: String?
        get() = if (backStack.size >= 2) backStack[backStack.size - 2].stableKey else null

    // App switcher state
    val screenshotStore = ScreenshotStore()
    private val _appSwitcherState = mutableStateOf(AppSwitcherState())
    val appSwitcherState: State<AppSwitcherState> = _appSwitcherState

    // Demo mode: saved real state to restore on dismiss
    private var savedBackStack: List<NavRoute>? = null
    val isDemoMode: Boolean get() = savedBackStack != null

    /** Navigation depth (0 for Home, 1 for first navigated screen, etc.) */
    val navigationDepth: Int
        get() = (backStack.size - 1).coerceAtLeast(0)

    fun navigate(route: NavRoute) {
        _navigationDirection.value = NavigationDirection.FORWARD
        // Capture current page screenshot before navigating away
        currentRoute?.let { current ->
            viewModelScope.launch {
                screenshotStore.captureOne(current.stableKey)
            }
        }
        backStack.add(route)
    }

    fun goBack() {
        _navigationDirection.value = NavigationDirection.BACKWARD
        if (backStack.size > 1) {
            val removed = backStack.removeLast()
            screenshotStore.remove(removed.stableKey)
        }
    }

    fun clearAndNavigate(route: NavRoute) {
        _navigationDirection.value = NavigationDirection.FORWARD
        // Clear all screenshots when resetting navigation
        backStack.forEach { screenshotStore.remove(it.stableKey) }
        backStack.clear()
        backStack.add(route)
    }

    fun showAppSwitcher() {
        // Capture the current page's latest screenshot, then show switcher
        viewModelScope.launch {
            screenshotStore.captureAll()
            _appSwitcherState.value = AppSwitcherState(
                isVisible = true,
                selectedIndex = backStack.size - 1
            )
        }
    }

    fun hideAppSwitcher() {
        _appSwitcherState.value = _appSwitcherState.value.copy(isVisible = false)
        // Restore real state if exiting demo mode
        savedBackStack?.let { saved ->
            backStack.clear()
            screenshotStore.clear()
            backStack.addAll(saved)
            savedBackStack = null
        }
    }

    fun updateSelectedIndex(index: Int) {
        _appSwitcherState.value = _appSwitcherState.value.copy(
            selectedIndex = index.coerceIn(0, (backStack.size - 1).coerceAtLeast(0))
        )
    }

    /** Navigate to the page at the given index, removing all pages after it.
     *  Does NOT hide the app switcher — the caller controls overlay dismissal. */
    fun navigateToIndex(index: Int) {
        if (isDemoMode) return // no-op in demo mode
        if (index < 0 || index >= backStack.size) return
        _navigationDirection.value = NavigationDirection.BACKWARD
        // Remove all routes after the target index
        while (backStack.size > index + 1) {
            val removed = backStack.removeLast()
            screenshotStore.remove(removed.stableKey)
        }
    }

    /** Remove the page at the given index (swipe-up delete) */
    fun removeAtIndex(index: Int) {
        if (index < 0 || index >= backStack.size || backStack.size <= 1) return
        val removed = backStack.removeAt(index)
        screenshotStore.remove(removed.stableKey)
        // Adjust selected index
        val newIndex = (index - 1).coerceAtLeast(0)
        updateSelectedIndex(newIndex)
        // If only one page left, close the switcher
        if (backStack.size <= 1) {
            hideAppSwitcher()
        }
    }

    /** Show the app switcher with 20 fake colored cards for testing. */
    fun showAppSwitcherDemo() {
        // Save real state
        savedBackStack = backStack.toList()

        // Replace with demo data
        backStack.clear()
        screenshotStore.clear()

        demoTitles.forEachIndexed { index, title ->
            val route = NavRoute.IllustDetail(
                illustId = -(index + 1).toLong(),
                title = title,
                previewUrl = "",
                aspectRatio = 1f
            )
            backStack.add(route)
            screenshotStore.putBitmap(route.stableKey, createColorBitmap(demoColors[index]))
        }

        _appSwitcherState.value = AppSwitcherState(
            isVisible = true,
            selectedIndex = backStack.size - 1
        )
    }

    companion object {
        private val demoTitles = listOf(
            "推荐插画", "每日排行", "关注动态", "热门标签",
            "新作速递", "精选特辑", "收藏夹", "浏览历史",
            "用户主页", "搜索结果", "小说阅读", "评论详情",
            "标签浏览", "动图合集", "缓存管理", "账号设置",
            "下载队列", "图片详情", "着色器演示", "以图搜图"
        )

        private val demoColors = listOf(
            Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFF5E35B1),
            Color(0xFF3949AB), Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1),
            Color(0xFF00897B), Color(0xFF43A047), Color(0xFF7CB342), Color(0xFFC0CA33),
            Color(0xFFFDD835), Color(0xFFFFB300), Color(0xFFFB8C00), Color(0xFFF4511E),
            Color(0xFF6D4C41), Color(0xFF546E7A), Color(0xFF26A69A), Color(0xFFAB47BC),
        )

        private fun createColorBitmap(color: Color, width: Int = 540, height: Int = 1170): ImageBitmap {
            val bitmap = ImageBitmap(width, height)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply { this.color = color }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            return bitmap
        }
    }
}

val LocalNavigationViewModel = compositionLocalOf<NavigationViewModel> {
    error("No NavigationViewModel provided")
}
