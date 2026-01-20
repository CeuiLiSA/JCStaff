package ceui.lisa.jcstaff.network

import ceui.lisa.jcstaff.core.Storable
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import java.io.Serializable

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
) : Serializable

data class HomeIllustResponse(
    val illusts: List<Illust> = listOf(),
    val ranking_illusts: List<Illust> = listOf(),
    val next_url: String? = null
) : Serializable {
    val displayList: List<Illust> get() = ranking_illusts + illusts
}

data class UserPreview(
    val illusts: List<Illust> = listOf(),
    val is_muted: Boolean? = null,
    val user: User? = null
) : Serializable

data class UserPreviewResponse(
    val user_previews: List<UserPreview> = listOf(),
    val next_url: String? = null
) : Serializable

data class TrendingTag(
    val tag: String? = null,
    val translated_name: String? = null,
    val illust: Illust? = null,
) : Serializable

data class TrendingTagsResponse(
    val trend_tags: List<TrendingTag> = listOf(),
    val next_url: String? = null
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