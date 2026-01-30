package ceui.lisa.jcstaff.tagdetail

import androidx.annotation.StringRes
import ceui.lisa.jcstaff.R

/**
 * 搜索排序选项
 */
enum class SearchSort(
    val apiValue: String,
    @StringRes val labelRes: Int,
    val premiumOnly: Boolean = false,
    val nonPremiumOnly: Boolean = false
) {
    POPULAR_PREVIEW("popular_preview", R.string.sort_popular_preview, nonPremiumOnly = true),
    DATE_DESC("date_desc", R.string.sort_date_desc),
    DATE_ASC("date_asc", R.string.sort_date_asc),
    POPULAR_DESC("popular_desc", R.string.sort_popular_desc, premiumOnly = true),
    POPULAR_MALE_DESC("popular_male_desc", R.string.sort_popular_male_desc, premiumOnly = true),
    POPULAR_FEMALE_DESC("popular_female_desc", R.string.sort_popular_female_desc, premiumOnly = true),
}

/**
 * 搜索匹配目标
 */
enum class SearchTarget(val apiValue: String, @StringRes val labelRes: Int) {
    PARTIAL_MATCH_FOR_TAGS("partial_match_for_tags", R.string.target_partial_tag),
    EXACT_MATCH_FOR_TAGS("exact_match_for_tags", R.string.target_exact_tag),
    TITLE_AND_CAPTION("title_and_caption", R.string.target_title_caption),
}
