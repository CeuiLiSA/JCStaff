package ceui.lisa.jcstaff.network

import ceui.lisa.jcstaff.core.Storable
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import java.io.Serializable

/**
 * 分页响应接口，所有支持分页的响应类都应实现此接口
 */
interface PagedResponse<T> {
    val displayList: List<T>
    val nextUrl: String?
}

data class AccountResponse(
    val access_token: String? = null,
    val expires_in: Int? = null,
    val refresh_token: String? = null,
    val scope: String? = null,
    val token_type: String? = null,
    val user: User? = null
) : Serializable

data class User(
    val account: String? = null,
    val id: Long = 0L,
    val is_followed: Boolean? = null,
    val name: String? = null,
    val profile_image_urls: ImageUrls? = null,
    val is_mail_authorized: Boolean? = null,
    val is_premium: Boolean? = null,
    val mail_address: String? = null,
    val comment: String? = null,
) : Serializable, Storable {
    override val storeKey: StoreKey get() = StoreKey(id, StoreType.USER)
}

data class ImageUrls(
    val large: String? = null,
    val medium: String? = null,
    val original: String? = null,
    val square_medium: String? = null,
    val px_170x170: String? = null,
    val px_50x50: String? = null,
) : Serializable {
    fun findMaxSizeUrl(): String? {
        return original ?: large ?: medium ?: square_medium ?: px_170x170 ?: px_50x50
    }

    /**
     * 获取用户头像 URL（优先使用 px_170x170，OAuth 返回的用户头像在此字段）
     */
    fun findAvatarUrl(): String? {
        return px_170x170 ?: medium ?: square_medium ?: px_50x50 ?: large
    }
}

data class Illust(
    val id: Long,
    val title: String? = null,
    val caption: String? = null,
    val create_date: String? = null,
    val height: Int = 0,
    val width: Int = 0,
    val image_urls: ImageUrls? = null,
    val is_bookmarked: Boolean? = null,
    val illust_ai_type: Int = 0,
    val meta_pages: List<MetaPage>? = null,
    val meta_single_page: MetaSinglePage? = null,
    val page_count: Int = 0,
    val restrict: Int? = null,
    val sanity_level: Int? = null,
    val tags: List<Tag>? = null,
    val total_bookmarks: Int? = null,
    val total_view: Int? = null,
    val type: String? = null,
    val user: User? = null,
    val visible: Boolean? = null,
    val x_restrict: Int? = null,
) : Serializable, Storable {

    override val storeKey: StoreKey get() = StoreKey(id, StoreType.ILLUST)

    fun isGif(): Boolean = type == "ugoira"

    fun isManga(): Boolean = type == "manga"

    fun maxUrl(): String? {
        return if (page_count == 1) {
            meta_single_page?.original_image_url
        } else {
            meta_pages?.firstOrNull()?.image_urls?.original
        }
    }

    /**
     * 获取列表中使用的预览图 URL
     */
    fun previewUrl(): String = image_urls?.large ?: image_urls?.medium ?: ""

    /**
     * 获取宽高比（用于 UI 展示）
     */
    fun aspectRatio(): Float {
        return if (width > 0 && height > 0) {
            (width.toFloat() / height.toFloat()).coerceIn(0.5f, 2f)
        } else {
            1f
        }
    }
}

data class MetaPage(
    val image_urls: ImageUrls? = null
) : Serializable

data class MetaSinglePage(
    val original_image_url: String? = null
) : Serializable

data class Tag(
    val name: String? = null,
    val translated_name: String? = null
) : Serializable

data class IllustResponse(
    val illusts: List<Illust> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<Illust> {
    override val displayList: List<Illust> get() = illusts
    override val nextUrl: String? get() = next_url
}

data class HomeIllustResponse(
    val illusts: List<Illust> = listOf(),
    val ranking_illusts: List<Illust> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<Illust> {
    override val displayList: List<Illust> get() = ranking_illusts + illusts
    override val nextUrl: String? get() = next_url
}

data class UserPreview(
    val illusts: List<Illust> = listOf(),
    val is_muted: Boolean? = null,
    val user: User? = null
) : Serializable

data class UserPreviewResponse(
    val user_previews: List<UserPreview> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<UserPreview> {
    override val displayList: List<UserPreview> get() = user_previews
    override val nextUrl: String? get() = next_url
}

data class Novel(
    val id: Long,
    val title: String? = null,
    val caption: String? = null,
    val create_date: String? = null,
    val image_urls: ImageUrls? = null,
    val is_bookmarked: Boolean? = null,
    val is_muted: Boolean? = null,
    val is_mypixiv_only: Boolean? = null,
    val is_original: Boolean? = null,
    val is_x_restricted: Boolean? = null,
    val page_count: Int? = null,
    val restrict: Int? = null,
    val series: NovelSeries? = null,
    val tags: List<Tag>? = null,
    val text_length: Int? = null,
    val total_bookmarks: Int? = null,
    val total_comments: Int? = null,
    val total_view: Int? = null,
    val user: User? = null,
    val visible: Boolean? = null,
    val x_restrict: Int? = null
) : Serializable, Storable {
    override val storeKey: StoreKey get() = StoreKey(id, StoreType.NOVEL)
}

data class NovelSeries(
    val id: Long = 0L,
    val title: String? = null
) : Serializable

data class NovelResponse(
    val novels: List<Novel> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<Novel> {
    override val displayList: List<Novel> get() = novels
    override val nextUrl: String? get() = next_url
}

data class TrendingTag(
    val tag: String? = null,
    val translated_name: String? = null,
    val illust: Illust? = null,
) : Serializable

data class TrendingTagsResponse(
    val trend_tags: List<TrendingTag> = listOf(),
    val next_url: String? = null
) : Serializable

data class AutocompleteResponse(
    val tags: List<Tag> = listOf()
) : Serializable

data class SingleNovelResponse(
    val novel: Novel? = null,
) : Serializable

data class SingleIllustResponse(
    val illust: Illust? = null,
) : Serializable

data class ErrorResponse(
    val error: Error? = null
) : Serializable

data class Error(
    val message: String? = null,
    val reason: String? = null,
    val user_message: String? = null,
) : Serializable {
    fun displayMessage(): String? = message ?: reason ?: user_message
}

data class UserDetailResponse(
    val user: User? = null,
    val profile: UserProfile? = null,
    val profile_publicity: ProfilePublicity? = null,
    val workspace: Workspace? = null
) : Serializable

data class UserProfile(
    val webpage: String? = null,
    val gender: String? = null,
    val birth: String? = null,
    val birth_day: String? = null,
    val birth_year: Int? = null,
    val region: String? = null,
    val address_id: Int? = null,
    val country_code: String? = null,
    val job: String? = null,
    val job_id: Int? = null,
    val total_follow_users: Int? = null,
    val total_mypixiv_users: Int? = null,
    val total_illusts: Int? = null,
    val total_manga: Int? = null,
    val total_novels: Int? = null,
    val total_illust_bookmarks_public: Int? = null,
    val total_illust_series: Int? = null,
    val total_novel_series: Int? = null,
    val background_image_url: String? = null,
    val twitter_account: String? = null,
    val twitter_url: String? = null,
    val pawoo_url: String? = null,
    val is_premium: Boolean? = null,
    val is_using_custom_profile_image: Boolean? = null
) : Serializable

data class ProfilePublicity(
    val gender: String? = null,
    val region: String? = null,
    val birth_day: String? = null,
    val birth_year: String? = null,
    val job: String? = null,
    val pawoo: Boolean? = null
) : Serializable

data class Workspace(
    val pc: String? = null,
    val monitor: String? = null,
    val tool: String? = null,
    val scanner: String? = null,
    val tablet: String? = null,
    val mouse: String? = null,
    val printer: String? = null,
    val desktop: String? = null,
    val music: String? = null,
    val desk: String? = null,
    val chair: String? = null,
    val comment: String? = null,
    val workspace_image_url: String? = null
) : Serializable

data class Comment(
    val comment: String? = null,
    val date: String? = null,
    val has_replies: Boolean = false,
    val id: Long = 0,
    val stamp: Stamp? = null,
    val user: User = User()
) : Serializable

data class Stamp(
    val stamp_id: Long = 0,
    val stamp_url: String? = null
) : Serializable

data class CommentResponse(
    val comments: List<Comment> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<Comment> {
    override val displayList: List<Comment> get() = comments
    override val nextUrl: String? get() = next_url
}

data class PostCommentResponse(
    val comment: Comment? = null
) : Serializable

// ===== Spotlight Models =====

data class SpotlightArticle(
    val id: Long = 0,
    val title: String? = null,
    val pure_title: String? = null,
    val thumbnail: String? = null,
    val article_url: String? = null,
    val publish_date: String? = null,
    val category: String? = null,
    val subcategory_label: String? = null
) : Serializable

data class SpotlightResponse(
    val spotlight_articles: List<SpotlightArticle> = listOf(),
    val next_url: String? = null
) : Serializable, PagedResponse<SpotlightArticle> {
    override val displayList: List<SpotlightArticle> get() = spotlight_articles
    override val nextUrl: String? get() = next_url
}

// ===== Ugoira Models =====

data class UgoiraResponse(
    val ugoira_metadata: UgoiraMetadata? = null
) : Serializable

data class UgoiraMetadata(
    val zip_urls: ZipUrls? = null,
    val frames: List<UgoiraFrame> = listOf()
) : Serializable {
    fun getZipUrl(): String? = zip_urls?.medium
}

data class ZipUrls(
    val medium: String? = null
) : Serializable

data class UgoiraFrame(
    val file: String? = null,
    val delay: Int = 60
) : Serializable

// ===== Bookmark Tags Models =====

data class BookmarkTagsResponse(
    val bookmark_tags: List<BookmarkTag> = emptyList(),
    val next_url: String? = null
) : Serializable, PagedResponse<BookmarkTag> {
    override val displayList: List<BookmarkTag> get() = bookmark_tags
    override val nextUrl: String? get() = next_url
}

data class BookmarkTag(
    val name: String? = null,
    val count: Int = 0
) : Serializable