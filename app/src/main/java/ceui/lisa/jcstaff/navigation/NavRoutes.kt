package ceui.lisa.jcstaff.navigation

import ceui.lisa.jcstaff.network.SpotlightArticle
import ceui.lisa.jcstaff.network.Tag

sealed interface NavRoute : java.io.Serializable {
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
    data object DownloadHistory : NavRoute {
        override val stableKey = "DownloadHistory"
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
    data class NovelReader(val novelId: Long, val novelTitle: String) : NavRoute {
        override val stableKey = "NovelReader_$novelId"
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
    data object LatestWorks : NavRoute {
        override val stableKey = "LatestWorks"
    }
    data class WebTagDetail(val tag: Tag) : NavRoute {
        override val stableKey = "WebTagDetail_${tag.name}"
    }
    data object BlockSettings : NavRoute {
        override val stableKey = "BlockSettings"
    }
    data class CacheBrowser(val initialPath: String? = null) : NavRoute {
        override val stableKey = "CacheBrowser_${initialPath ?: "root"}"
    }
    data object SauceNao : NavRoute {
        override val stableKey = "SauceNao"
    }
    data object AppSwitcherDemo : NavRoute {
        override val stableKey = "AppSwitcherDemo"
    }
    data class DemoPage(val title: String) : NavRoute {
        override val stableKey = "DemoPage_$title"
    }
    data class UserCreatedIllusts(val userId: Long, val type: String) : NavRoute {
        override val stableKey = "UserCreatedIllusts_${userId}_$type"
    }
    data class UserCreatedNovels(val userId: Long) : NavRoute {
        override val stableKey = "UserCreatedNovels_$userId"
    }
    data class UserBookmarkNovels(val userId: Long) : NavRoute {
        override val stableKey = "UserBookmarkNovels_$userId"
    }
    data class UserFollowing(val userId: Long) : NavRoute {
        override val stableKey = "UserFollowing_$userId"
    }
    data class CollectionDetail(val collectionId: String) : NavRoute {
        override val stableKey = "CollectionDetail_$collectionId"
    }
    data object CollectionDiscovery : NavRoute {
        override val stableKey = "CollectionDiscovery"
    }
    data class Report(val objectId: Long, val objectType: String) : NavRoute {
        override val stableKey = "Report_${objectType}_$objectId"
    }
    data class MangaReader(
        val illustId: Long,
        val illustTitle: String,
        val initialPage: Int = 0
    ) : NavRoute {
        override val stableKey = "MangaReader_$illustId"
    }
    data class NovelSeriesDetail(val seriesId: Long, val seriesTitle: String) : NavRoute {
        override val stableKey = "NovelSeriesDetail_$seriesId"
    }
    data class IllustSeriesDetail(val seriesId: Long, val seriesTitle: String) : NavRoute {
        override val stableKey = "IllustSeriesDetail_$seriesId"
    }
}

/** Get a human-readable title for the route (used in app switcher) */
fun NavRoute.getTitle(): String = when (this) {
    is NavRoute.Landing -> "登录"
    is NavRoute.Home -> "首页"
    is NavRoute.Search -> "搜索"
    is NavRoute.IllustDetail -> title.ifEmpty { "作品详情" }
    is NavRoute.Bookmarks -> "收藏"
    is NavRoute.Settings -> "设置"
    is NavRoute.ImageViewer -> "图片查看"
    is NavRoute.BrowseHistory -> "浏览历史"
    is NavRoute.DownloadHistory -> "下载历史"
    is NavRoute.UserProfile -> "用户主页"
    is NavRoute.TagDetail -> tag.name ?: "标签"
    is NavRoute.NovelDetail -> "小说详情"
    is NavRoute.NovelReader -> novelTitle.ifEmpty { "阅读小说" }
    is NavRoute.ShaderDemo -> "着色器演示"
    is NavRoute.AccountManagement -> "账号管理"
    is NavRoute.CommentDetail -> "评论"
    is NavRoute.RankingDetail -> "排行榜"
    is NavRoute.SpotlightDetail -> article.title ?: "特辑"
    is NavRoute.UgoiraRanking -> "动图排行"
    is NavRoute.LatestWorks -> "最新作品"
    is NavRoute.WebTagDetail -> tag.name ?: "标签详情"
    is NavRoute.BlockSettings -> "屏蔽设置"
    is NavRoute.CacheBrowser -> "缓存浏览"
    is NavRoute.SauceNao -> "以图搜图"
    is NavRoute.UserCreatedIllusts -> if (type == "illust") "插画作品" else "漫画作品"
    is NavRoute.UserCreatedNovels -> "小说作品"
    is NavRoute.UserBookmarkNovels -> "收藏小说"
    is NavRoute.UserFollowing -> "关注用户"
    is NavRoute.AppSwitcherDemo -> "页面切换演示"
    is NavRoute.DemoPage -> title
    is NavRoute.CollectionDetail -> "珍藏册"
    is NavRoute.CollectionDiscovery -> "珍藏册发现"
    is NavRoute.Report -> if (objectType == "user") "举报用户" else "举报作品"
    is NavRoute.MangaReader -> illustTitle.ifEmpty { "漫画阅读" }
    is NavRoute.NovelSeriesDetail -> seriesTitle.ifEmpty { "系列" }
    is NavRoute.IllustSeriesDetail -> seriesTitle.ifEmpty { "漫画系列" }
}