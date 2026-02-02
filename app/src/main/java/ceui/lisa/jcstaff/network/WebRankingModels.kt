package ceui.lisa.jcstaff.network

import java.io.Serializable

/**
 * 网页版排行榜项目（从 HTML 的 __NEXT_DATA__ 解析）
 */
data class WebRankingItem(
    val illust_id: Long = 0,
    val title: String? = null,
    val date: String? = null,
    val tags: List<String>? = null,
    val url: String? = null,
    val illust_type: String? = null,
    val illust_page_count: String? = null,
    val user_name: String? = null,
    val profile_img: String? = null,
    val user_id: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val rank: Int = 0,
    val yes_rank: Int = 0,
    val rating_count: Int = 0,
    val view_count: Int = 0,
    val illust_upload_timestamp: Long = 0,
    val attr: String? = null
) : Serializable {

    /**
     * 转换为 Illust 对象，以便复用现有 UI 组件
     */
    fun toIllust(): Illust {
        // 构建大图 URL：将 /c/480x960/img-master/ 替换为 /c/540x540_70/img-master/
        val largeUrl = url?.replace("/c/480x960/", "/c/540x540_70/")

        return Illust(
            id = illust_id,
            title = title,
            width = width,
            height = height,
            type = when (illust_type) {
                "2" -> "ugoira"
                "1" -> "manga"
                else -> "illust"
            },
            page_count = illust_page_count?.toIntOrNull() ?: 1,
            total_bookmarks = rating_count,
            total_view = view_count,
            image_urls = ImageUrls(
                medium = url,
                large = largeUrl,
                square_medium = url
            ),
            user = User(
                id = user_id,
                name = user_name,
                profile_image_urls = ImageUrls(
                    medium = profile_img,
                    px_170x170 = profile_img
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

/**
 * 网页排行榜响应（__NEXT_DATA__ 结构）
 */
data class WebRankingResponse(
    val props: WebRankingProps? = null
) : Serializable

data class WebRankingProps(
    val pageProps: WebRankingPageProps? = null
) : Serializable

data class WebRankingPageProps(
    val assign: WebRankingAssign? = null
) : Serializable

data class WebRankingAssign(
    val contents: List<WebRankingItem>? = null,
    val mode: String? = null,
    val content: String? = null,
    val date: String? = null,
    val prev_date: String? = null,
    val next_date: String? = null,
    val prev_date_text: String? = null,
    val next_date_text: String? = null
) : Serializable