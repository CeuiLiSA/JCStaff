package ceui.lisa.jcstaff.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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
    }

    fun updateSelectedIndex(index: Int) {
        _appSwitcherState.value = _appSwitcherState.value.copy(
            selectedIndex = index.coerceIn(0, (backStack.size - 1).coerceAtLeast(0))
        )
    }

    /** Navigate to the page at the given index, removing all pages after it.
     *  Does NOT hide the app switcher — the caller controls overlay dismissal. */
    fun navigateToIndex(index: Int) {
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
}

val LocalNavigationViewModel = compositionLocalOf<NavigationViewModel> {
    error("No NavigationViewModel provided")
}
