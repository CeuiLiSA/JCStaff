package ceui.lisa.jcstaff.network

data class AmWork(
    val title: String,
    val user: String,
    val artworkLink: String,
    val userLink: String,
    val userImage: String,
    val showImage: String,
    val pageCount: Int = 1
) {
    /**
     * Extract illust ID from artwork link
     * e.g., "https://www.pixiv.net/artworks/12345" -> 12345
     */
    fun getIllustId(): Long? {
        return try {
            val cleaned = artworkLink.substringBefore("?").substringBefore("#")
            val segments = cleaned.split("/")
            val artworksIndex = segments.indexOf("artworks")
            if (artworksIndex >= 0 && artworksIndex < segments.size - 1) {
                segments[artworksIndex + 1].toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract user ID from user link
     * e.g., "https://www.pixiv.net/users/12345" -> 12345
     */
    fun getUserId(): Long? {
        return try {
            val cleaned = userLink.substringBefore("?").substringBefore("#")
            val segments = cleaned.split("/")
            val usersIndex = segments.indexOf("users")
            if (usersIndex >= 0 && usersIndex < segments.size - 1) {
                segments[usersIndex + 1].toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

sealed interface ArticleContent {
    data class Paragraph(val text: String) : ArticleContent
    data class Work(val amWork: AmWork) : ArticleContent
    data class RelatedArticle(
        val title: String,
        val url: String,
        val thumbnailUrl: String?
    ) : ArticleContent
}
