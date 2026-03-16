package ceui.lisa.jcstaff.core

import ceui.lisa.jcstaff.network.Illust

object FilenameFormatter {

    /**
     * Format a filename from a template string and illustration metadata.
     *
     * Supported variables:
     * - {id}      — illustration ID
     * - {title}   — illustration title
     * - {user}    — author display name
     * - {user_id} — author user ID
     * - {page}    — page number (0-indexed)
     * - {date}    — creation date (yyyy-MM-dd)
     *
     * @param template  the user-defined template string
     * @param illust    the illustration whose metadata populates the variables
     * @param pageIndex 0-based page index
     * @return a filesystem-safe filename (without extension)
     */
    fun format(template: String, illust: Illust, pageIndex: Int = 0): String {
        val date = illust.create_date?.take(10) ?: ""  // yyyy-MM-dd
        val result = template
            .replace("{id}", illust.id.toString())
            .replace("{title}", sanitize(illust.title ?: "untitled"))
            .replace("{user}", sanitize(illust.user?.name ?: "unknown"))
            .replace("{user_id}", (illust.user?.id ?: 0).toString())
            .replace("{page}", pageIndex.toString())
            .replace("{date}", date)

        // If the template produced an empty or blank string, fall back to timestamp
        return result.ifBlank { "pixiv_${illust.id}_${System.currentTimeMillis()}" }
    }

    /**
     * Remove characters that are illegal in most file systems.
     * Also trims the result to 100 characters to avoid path-length issues.
     */
    private fun sanitize(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
    }
}
