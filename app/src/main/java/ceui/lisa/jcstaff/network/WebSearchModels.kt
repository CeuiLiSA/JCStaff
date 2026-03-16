package ceui.lisa.jcstaff.network

import java.io.Serializable

/**
 * 网页版搜索 API 响应模型
 * API: /ajax/search/top/{tag}
 */
data class WebSearchResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: WebSearchBody? = null
) : Serializable

data class WebSearchBody(
    val illust: WebSearchIllustData? = null,
    val manga: WebSearchIllustData? = null,
    val novel: WebSearchNovelData? = null,
    val popular: WebSearchPopular? = null,
    val relatedTags: List<String>? = null,
    val tagTranslation: Map<String, WebTagTranslation>? = null
) : Serializable

data class WebSearchIllustData(
    val data: List<WebIllust>? = null,
    val total: Int = 0
) : Serializable

data class WebSearchNovelData(
    val data: List<WebNovel>? = null,
    val total: Int = 0
) : Serializable

data class WebSearchPopular(
    val recent: List<WebIllust>? = null,
    val permanent: List<WebIllust>? = null
) : Serializable

/**
 * 网页版插画数据
 */
data class WebIllust(
    val id: String? = null,
    val title: String? = null,
    val illustType: Int = 0,  // 0=illust, 1=manga, 2=ugoira
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val sl: Int = 0,
    val url: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val userId: String? = null,
    val userName: String? = null,
    val profileImageUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val pageCount: Int = 1,
    val isBookmarkable: Boolean = true,
    val bookmarkData: WebBookmarkData? = null,
    val alt: String? = null,
    val titleCaptionTranslation: WebTitleCaptionTranslation? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val isUnlisted: Boolean = false,
    val isMasked: Boolean = false,
    val aiType: Int = 0,  // 0=unknown, 1=not_ai, 2=ai_generated
    val visibilityScope: Int = 0,
    val urls: Map<String, String>? = null,
    val seriesId: String? = null,
    val seriesTitle: String? = null
) : Serializable {

    /**
     * 转换为 Illust 对象
     */
    fun toIllust(): Illust {
        val illustId = id?.toLongOrNull() ?: 0L
        // 优先用 url 字段，fallback 到 urls map
        val squareUrl = url ?: urls?.get("250x250") ?: urls?.get("360x360") ?: urls?.values?.firstOrNull()
        // 构建大图 URL
        val largeUrl = (url ?: urls?.get("540x540") ?: urls?.get("1200x1200") ?: squareUrl)
            ?.replace("/c/250x250_80_a2/", "/c/540x540_70/")
            ?.replace("/c/360x360_70/", "/c/540x540_70/")

        return Illust(
            id = illustId,
            title = title,
            caption = description,
            width = width,
            height = height,
            type = when (illustType) {
                2 -> "ugoira"
                1 -> "manga"
                else -> "illust"
            },
            page_count = pageCount,
            is_bookmarked = bookmarkData != null,
            illust_ai_type = aiType,
            create_date = createDate,
            image_urls = ImageUrls(
                medium = squareUrl,
                large = largeUrl,
                square_medium = squareUrl
            ),
            user = User(
                id = userId?.toLongOrNull() ?: 0L,
                name = userName,
                profile_image_urls = ImageUrls(
                    medium = profileImageUrl,
                    px_170x170 = profileImageUrl
                )
            ),
            tags = tags?.map { Tag(name = it) }
        )
    }

    fun aspectRatio(): Float {
        return if (width > 0 && height > 0) {
            (width.toFloat() / height.toFloat()).coerceIn(0.5f, 2f)
        } else {
            1f
        }
    }
}

data class WebBookmarkData(
    val id: String? = null,
    val private: Boolean = false
) : Serializable

data class WebTitleCaptionTranslation(
    val workTitle: String? = null,
    val workCaption: String? = null
) : Serializable

/**
 * 网页版小说数据
 */
data class WebNovel(
    val id: String? = null,
    val title: String? = null,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String? = null,
    val tags: List<String>? = null,
    val userId: String? = null,
    val userName: String? = null,
    val profileImageUrl: String? = null,
    val textCount: Int = 0,
    val wordCount: Int = 0,
    val readingTime: Int = 0,
    val description: String? = null,
    val isBookmarkable: Boolean = true,
    val bookmarkData: WebBookmarkData? = null,
    val bookmarkCount: Int = 0,
    val createDate: String? = null,
    val updateDate: String? = null,
    val seriesId: String? = null,
    val seriesTitle: String? = null,
    val aiType: Int = 0
) : Serializable {

    /**
     * 转换为 Novel 对象
     */
    fun toNovel(): Novel {
        return Novel(
            id = id?.toLongOrNull() ?: 0L,
            title = title,
            caption = description,
            is_bookmarked = bookmarkData != null,
            text_length = textCount,
            total_bookmarks = bookmarkCount,
            create_date = createDate,
            image_urls = ImageUrls(medium = url),
            user = User(
                id = userId?.toLongOrNull() ?: 0L,
                name = userName,
                profile_image_urls = ImageUrls(
                    medium = profileImageUrl,
                    px_170x170 = profileImageUrl
                )
            ),
            tags = tags?.map { Tag(name = it) },
            series = seriesId?.let {
                NovelSeries(
                    id = it.toLongOrNull() ?: 0L,
                    title = seriesTitle
                )
            }
        )
    }
}

/**
 * 标签翻译
 */
data class WebTagTranslation(
    val en: String? = null,
    val ko: String? = null,
    val zh: String? = null,
    val zh_tw: String? = null,
    val romaji: String? = null
) : Serializable

// ==================== Tag Info API ====================

/**
 * 标签信息 API 响应
 * API: /ajax/search/tags/{tag}
 */
data class WebTagInfoResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: WebTagInfoBody? = null
) : Serializable

data class WebTagInfoBody(
    val tag: String? = null,
    val word: String? = null,
    val pixpedia: WebPixpedia? = null,
    val breadcrumbs: WebBreadcrumbs? = null,
    val myFavoriteTags: List<String>? = null,
    val tagTranslation: Map<String, WebTagTranslation>? = null
) : Serializable

data class WebPixpedia(
    val id: String? = null,
    val `abstract`: String? = null,
    val image: String? = null,
    val yomigana: String? = null,
    val parentTag: String? = null,
    val siblingsTags: List<String>? = null,
    val childrenTags: List<String>? = null
) : Serializable

data class WebBreadcrumbs(
    val successor: List<WebBreadcrumbItem>? = null,
    val current: List<WebBreadcrumbItem>? = null  // API 返回数组
) : Serializable

data class WebBreadcrumbItem(
    val tag: String? = null,
    val url: String? = null
) : Serializable
