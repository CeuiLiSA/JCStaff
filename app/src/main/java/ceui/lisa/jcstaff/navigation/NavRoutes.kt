package ceui.lisa.jcstaff.navigation

import ceui.lisa.jcstaff.network.Tag

sealed interface NavRoute {
    data object Landing : NavRoute
    data object Home : NavRoute
    data object Search : NavRoute
    data class IllustDetail(
        val illustId: Long,
        val title: String,
        val previewUrl: String,
        val aspectRatio: Float
    ) : NavRoute
    data class Bookmarks(val userId: Long) : NavRoute
    data object Settings : NavRoute
    data class ImageViewer(
        val imageUrl: String,
        val originalUrl: String?,
        val sharedElementKey: String
    ) : NavRoute
    data object BrowseHistory : NavRoute
    data class UserProfile(val userId: Long) : NavRoute
    data class TagDetail(val tag: Tag) : NavRoute
    data class NovelDetail(val novelId: Long) : NavRoute
}