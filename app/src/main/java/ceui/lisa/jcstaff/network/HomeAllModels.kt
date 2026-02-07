package ceui.lisa.jcstaff.network

import com.google.gson.annotations.SerializedName

/**
 * POST /v1/home/all 请求体
 */
data class HomeAllReq(
    val vhi: List<String> = emptyList(),
    val vhm: List<String> = emptyList(),
    val vhn: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val next_params: NextPageSpec? = null
)

/**
 * 分页 token
 */
data class NextPageSpec(
    val page: Int = 0,
    val content_index_prev: Int = 0,
    val li: String? = null,
    val lm: String? = null,
    val ln: String? = null,
    val lc: String? = null
)

/**
 * POST /v1/home/all 响应
 */
data class HomeAllResponse(
    val contents: List<HomeContent> = emptyList(),
    @SerializedName("next_params")
    val nextParams: NextPageSpec? = null,
    val states: List<Any>? = null
)

/**
 * 一个内容块（插画、漫画、小说、标签轮播、分隔符）
 */
data class HomeContent(
    val kind: String? = null,
    val thumbnails: List<ThumbnailItem>? = null,
    val pickup: Pickup? = null,
    val access: Any? = null
)

/**
 * 缩略图项，app_model 中包含原始 Illust/Novel 的 JSON 数据
 */
data class ThumbnailItem(
    val type: String? = null,
    val id: Long = 0,
    val title: String? = null,
    val user_id: Long = 0,
    val user_name: String? = null,
    val profile_image_url: String? = null,
    val app_model: Any? = null
)

/**
 * 评论区信息（Instagram 风格）
 */
data class Pickup(
    val type: String? = null,
    val user_id: Long = 0,
    val user_name: String? = null,
    val profile_image_url: String? = null,
    val comment_id: Long = 0,
    val comment: String? = null,
    val comment_count: Int = 0
)
