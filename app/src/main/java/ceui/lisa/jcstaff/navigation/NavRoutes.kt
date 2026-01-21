package ceui.lisa.jcstaff.navigation

sealed interface NavRoute {
    data object Landing : NavRoute
    data class Login(val isSignup: Boolean = false) : NavRoute
    data object Home : NavRoute
    data class IllustDetail(
        val illustId: Long,
        val title: String,
        val previewUrl: String,
        val aspectRatio: Float
    ) : NavRoute
    data class Bookmarks(val userId: Long) : NavRoute
    data object Settings : NavRoute
}