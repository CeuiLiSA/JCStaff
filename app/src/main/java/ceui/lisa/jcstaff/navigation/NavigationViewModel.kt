package ceui.lisa.jcstaff.navigation

import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.components.appswitcher.ScreenshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

enum class NavigationDirection { FORWARD, BACKWARD }

data class AppSwitcherState(
    val isVisible: Boolean = false,
    val selectedIndex: Int = -1
)

/**
 * Wraps a [NavRoute] with a unique ID so the same route appearing multiple
 * times in the back stack (e.g. UserProfile → IllustDetail → UserProfile)
 * gets distinct screenshot keys.
 */
data class BackStackEntry(
    val id: Int,
    val route: NavRoute
) {
    /** Unique key for screenshot storage — distinguishes duplicate routes. */
    val screenshotKey: String get() = "${id}_${route.stableKey}"
}

class NavigationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 使用 StateFlow 替代 SnapshotStateList，确保线程安全
    private val _backStack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    val backStack: StateFlow<List<BackStackEntry>> = _backStack.asStateFlow()

    private val nextEntryId = AtomicInteger(0)

    /** 导航是否就绪（用于 SplashScreen 条件判断，StateFlow 线程安全） */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** 是否可以返回（StateFlow 线程安全） */
    val canGoBack: StateFlow<Boolean> = MutableStateFlow(false)
    private val _canGoBack get() = canGoBack as MutableStateFlow

    /** 当前 entry（StateFlow 线程安全） */
    private val _currentEntry = MutableStateFlow<BackStackEntry?>(null)
    val currentEntry: StateFlow<BackStackEntry?> = _currentEntry.asStateFlow()

    val currentRoute: NavRoute?
        get() = _currentEntry.value?.route

    // Navigation direction for transition animations
    private val _navigationDirection = MutableStateFlow(NavigationDirection.FORWARD)
    val navigationDirection: StateFlow<NavigationDirection> = _navigationDirection.asStateFlow()

    val previousRouteKey: String?
        get() {
            val stack = _backStack.value
            return if (stack.size >= 2) stack[stack.size - 2].screenshotKey else null
        }

    // App switcher state
    val screenshotStore = ScreenshotStore()
    private val _appSwitcherState = MutableStateFlow(AppSwitcherState())
    val appSwitcherState: StateFlow<AppSwitcherState> = _appSwitcherState.asStateFlow()

    /** Navigation depth (0 for Home, 1 for first navigated screen, etc.) */
    val navigationDepth: Int
        get() = (_backStack.value.size - 1).coerceAtLeast(0)

    init {
        restoreBackStack()
    }

    private fun updateDerivedState() {
        val stack = _backStack.value
        _canGoBack.value = stack.size > 1
        _currentEntry.value = stack.lastOrNull()
    }

    private fun restoreBackStack() {
        try {
            @Suppress("UNCHECKED_CAST")
            val savedRoutes = savedStateHandle.get<ArrayList<NavRoute>>(KEY_BACK_STACK)
            if (!savedRoutes.isNullOrEmpty()) {
                _backStack.value = savedRoutes.map {
                    BackStackEntry(nextEntryId.getAndIncrement(), it)
                }
                updateDerivedState()
                _isReady.value = true
            }
        } catch (e: Exception) {
            Log.w("NavigationViewModel", "Failed to restore back stack", e)
        }
    }

    private fun saveBackStack() {
        val routes = _backStack.value.map { it.route }
        savedStateHandle[KEY_BACK_STACK] = ArrayList(routes)
    }

    fun navigate(route: NavRoute) {
        _navigationDirection.value = NavigationDirection.FORWARD
        // Capture current page screenshot before navigating away
        _currentEntry.value?.let { current ->
            viewModelScope.launch {
                screenshotStore.captureOne(current.screenshotKey)
            }
        }
        _backStack.value = _backStack.value + BackStackEntry(nextEntryId.getAndIncrement(), route)
        updateDerivedState()
        saveBackStack()
    }

    fun goBack() {
        _navigationDirection.value = NavigationDirection.BACKWARD
        val stack = _backStack.value
        if (stack.size > 1) {
            val removed = stack.last()
            _backStack.value = stack.dropLast(1)
            screenshotStore.remove(removed.screenshotKey)
            updateDerivedState()
            saveBackStack()
        }
    }

    fun clearAndNavigate(route: NavRoute) {
        _navigationDirection.value = NavigationDirection.FORWARD
        // Clear all screenshots when resetting navigation
        _backStack.value.forEach { screenshotStore.remove(it.screenshotKey) }
        _backStack.value = listOf(BackStackEntry(nextEntryId.getAndIncrement(), route))
        updateDerivedState()
        saveBackStack()
        _isReady.value = true
    }

    fun showAppSwitcher() {
        // Capture the current page's latest screenshot, then show switcher
        viewModelScope.launch {
            screenshotStore.captureAll()
            _appSwitcherState.value = AppSwitcherState(
                isVisible = true,
                selectedIndex = _backStack.value.size - 1
            )
        }
    }

    fun hideAppSwitcher() {
        _appSwitcherState.value = _appSwitcherState.value.copy(isVisible = false)
    }

    fun updateSelectedIndex(index: Int) {
        _appSwitcherState.value = _appSwitcherState.value.copy(
            selectedIndex = index.coerceIn(0, (_backStack.value.size - 1).coerceAtLeast(0))
        )
    }

    /** Navigate to the page at the given index, removing all pages after it.
     *  Does NOT hide the app switcher — the caller controls overlay dismissal. */
    fun navigateToIndex(index: Int) {
        val stack = _backStack.value
        if (index < 0 || index >= stack.size) return
        _navigationDirection.value = NavigationDirection.BACKWARD
        // Remove all routes after the target index
        val toRemove = stack.drop(index + 1)
        toRemove.forEach { screenshotStore.remove(it.screenshotKey) }
        _backStack.value = stack.take(index + 1)
        updateDerivedState()
        saveBackStack()
    }

    /** Remove the page at the given index (swipe-up delete) */
    fun removeAtIndex(index: Int) {
        val stack = _backStack.value
        if (index < 0 || index >= stack.size || stack.size <= 1) return
        val removed = stack[index]
        _backStack.value = stack.filterIndexed { i, _ -> i != index }
        screenshotStore.remove(removed.screenshotKey)
        updateDerivedState()
        saveBackStack()
        // Adjust selected index
        val newIndex = (index - 1).coerceAtLeast(0)
        updateSelectedIndex(newIndex)
        // If only one page left, close the switcher
        if (_backStack.value.size <= 1) {
            hideAppSwitcher()
        }
    }

    companion object {
        private const val KEY_BACK_STACK = "nav_back_stack"
    }

}

val LocalNavigationViewModel = compositionLocalOf<NavigationViewModel> {
    error("No NavigationViewModel provided")
}
