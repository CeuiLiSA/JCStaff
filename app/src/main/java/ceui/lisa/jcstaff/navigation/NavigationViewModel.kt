package ceui.lisa.jcstaff.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class NavigationViewModel : ViewModel() {

    val backStack: SnapshotStateList<NavRoute> = mutableStateListOf()

    val currentRoute: NavRoute?
        get() = backStack.lastOrNull()

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigate(route: NavRoute) {
        backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    fun clearAndNavigate(route: NavRoute) {
        backStack.clear()
        backStack.add(route)
    }
}

val LocalNavigationViewModel = compositionLocalOf<NavigationViewModel> {
    error("No NavigationViewModel provided")
}
