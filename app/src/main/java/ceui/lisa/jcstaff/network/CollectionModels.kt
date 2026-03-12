package ceui.lisa.jcstaff.network

import java.io.Serializable

/**
 * 珍藏册 API 响应模型
 * API: /ajax/collection/{collectionId}
 */
data class CollectionResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: CollectionBody? = null
) : Serializable

data class CollectionBody(
    val tagTranslation: Map<String, WebTagTranslation>? = null,
    val thumbnails: CollectionThumbnails? = null,
    val users: List<CollectionUser>? = null,
    val data: CollectionData? = null,
    val extraData: CollectionExtraData? = null
) : Serializable

data class CollectionThumbnails(
    val illust: List<WebIllust>? = null,
    val collection: List<CollectionSummary>? = null
) : Serializable

data class CollectionUser(
    val userId: String? = null,
    val name: String? = null,
    val image: String? = null,
    val imageBig: String? = null,
    val premium: Boolean = false,
    val isFollowed: Boolean = false,
    val isMypixiv: Boolean = false,
    val isBlocking: Boolean = false
) : Serializable

data class CollectionData(
    val detail: CollectionDetail? = null
) : Serializable

data class CollectionDetail(
    val tags: CollectionTags? = null,
    val tiles: List<CollectionTile>? = null,
    val userCollections: Map<String, CollectionSummary>? = null
) : Serializable

data class CollectionTags(
    val authorId: String? = null,
    val isLocked: Boolean = false,
    val tags: List<CollectionTagItem>? = null
) : Serializable

data class CollectionTagItem(
    val tag: String? = null,
    val locked: Boolean = false
) : Serializable

data class CollectionTile(
    val id: String? = null,
    val layout: TileLayout? = null,
    val status: String? = null,
    val type: String? = null,       // "Work" or "Text"
    val workType: String? = null,   // "illust" etc.
    val workId: String? = null,
    val text: String? = null,
    val background: String? = null,
    val textSize: String? = null,
    val align: String? = null,
    val fontType: String? = null
) : Serializable

data class TileLayout(
    val position: TilePosition? = null,
    val size: TilePosition? = null
) : Serializable

data class TilePosition(
    val x: Int = 0,
    val y: Int = 0
) : Serializable

/**
 * 珍藏册摘要（用于列表展示和相关珍藏册）
 */
data class CollectionSummary(
    val id: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val profileImageUrl: String? = null,
    val title: String? = null,
    val tags: List<String>? = null,
    val caption: String? = null,
    val language: String? = null,
    val visibilityScope: Int = 0,
    val xRestrict: Int = 0,
    val sl: Int = 0,
    val commentOff: Boolean = false,
    val isSpoiler: Boolean = false,
    val isBookmarkable: Boolean = true,
    val bookmarkData: WebBookmarkData? = null,
    val bookmarkCount: Int = 0,
    val viewCount: Int = 0,
    val citedDataHash: String? = null,
    val thumbnailImageUrl: String? = null,
    val status: String? = null,
    val publishedDateTime: String? = null
) : Serializable

data class CollectionExtraData(
    val meta: CollectionMeta? = null
) : Serializable

data class CollectionMeta(
    val title: String? = null,
    val description: String? = null,
    val canonical: String? = null
) : Serializable

/**
 * 用户 profile/all 响应中的 collectionIds
 */
data class UserProfileAllResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: UserProfileAllBody? = null
) : Serializable

data class UserProfileAllBody(
    val collectionIds: List<String>? = null
) : Serializable

// ==================== 珍藏册搜索 API 模型 ====================

/**
 * 推荐标签响应
 * API: /ajax/collections/search/recommended_tags
 */
data class CollectionRecommendedTagsResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: CollectionRecommendedTagsBody? = null
) : Serializable

data class CollectionRecommendedTagsBody(
    val recommendedTags: List<String>? = null,
    val tagTranslation: Map<String, WebTagTranslation>? = null
) : Serializable

/**
 * 珍藏册搜索响应
 * API: /ajax/collections/search
 */
data class CollectionSearchResponse(
    val error: Boolean = false,
    val message: String? = null,
    val body: CollectionSearchBody? = null
) : Serializable

data class CollectionSearchBody(
    val tagTranslation: Map<String, WebTagTranslation>? = null,
    val thumbnails: CollectionSearchThumbnails? = null,
    val users: List<CollectionUser>? = null,
    val data: CollectionSearchData? = null
) : Serializable

data class CollectionSearchThumbnails(
    val illust: List<WebIllust>? = null,
    val collection: List<CollectionSummary>? = null
) : Serializable

data class CollectionSearchData(
    val ids: List<String>? = null,
    val total: Int = 0
) : Serializable

// ==================== 发现页结果 ====================

data class CollectionTagGroup(
    val tag: String = "",
    val total: Int = 0,
    val collections: List<CollectionSummary> = emptyList()
) : Serializable
