package ceui.lisa.jcstaff.navigation

import ceui.lisa.jcstaff.network.SpotlightArticle
import ceui.lisa.jcstaff.network.Tag

sealed interface NavRoute {
    /** Bundle-compatible key for SaveableStateProvider */
    val stableKey: String

    data object Landing : NavRoute {
        override val stableKey = "Landing"
    }
    data object Home : NavRoute {
        override val stableKey = "Home"
    }
    data object Search : NavRoute {
        override val stableKey = "Search"
    }
    data class IllustDetail(
        val illustId: Long,
        val title: String,
        val previewUrl: String,
        val aspectRatio: Float
    ) : NavRoute {
        override val stableKey = "IllustDetail_$illustId"
    }
    data class Bookmarks(val userId: Long) : NavRoute {
        override val stableKey = "Bookmarks_$userId"
    }
    data object Settings : NavRoute {
        override val stableKey = "Settings"
    }
    data class ImageViewer(
        val imageUrl: String,
        val originalUrl: String?,
        val sharedElementKey: String
    ) : NavRoute {
        override val stableKey = "ImageViewer_$sharedElementKey"
    }
    data object BrowseHistory : NavRoute {
        override val stableKey = "BrowseHistory"
    }
    data class UserProfile(val userId: Long) : NavRoute {
        override val stableKey = "UserProfile_$userId"
    }
    data class TagDetail(val tag: Tag, val initialTab: Int = 0) : NavRoute {
        override val stableKey = "TagDetail_${tag.name}"
    }
    data class NovelDetail(val novelId: Long) : NavRoute {
        override val stableKey = "NovelDetail_$novelId"
    }
    data object ShaderDemo : NavRoute {
        override val stableKey = "ShaderDemo"
    }
    data object AccountManagement : NavRoute {
        override val stableKey = "AccountManagement"
    }
    data class CommentDetail(
        val objectId: Long,
        val objectType: String  // "illust" or "novel"
    ) : NavRoute {
        override val stableKey = "CommentDetail_${objectType}_$objectId"
    }
    data class RankingDetail(
        val objectType: String  // "illust" or "manga"
    ) : NavRoute {
        override val stableKey = "RankingDetail_$objectType"
    }
    data class SpotlightDetail(
        val article: SpotlightArticle
    ) : NavRoute {
        override val stableKey = "SpotlightDetail_${article.id}"
    }
    data object UgoiraRanking : NavRoute {
        override val stableKey = "UgoiraRanking"
    }
}