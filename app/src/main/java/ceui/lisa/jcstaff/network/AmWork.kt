package ceui.lisa.jcstaff.network

data class AmWork(
    val title: String,
    val user: String,
    val artworkLink: String,
    val userLink: String,
    val userImage: String,
    val showImage: String
) {
    /**
     * Extract illust ID from artwork link
     * e.g., "https://www.pixiv.net/artworks/12345" -> 12345
     */
    fun getIllustId(): Long? {
        return try {
            val segments = artworkLink.split("/")
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
            val segments = userLink.split("/")
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
