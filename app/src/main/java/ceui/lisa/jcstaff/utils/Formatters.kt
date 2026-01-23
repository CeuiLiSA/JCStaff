package ceui.lisa.jcstaff.utils

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 格式化数字显示（用于收藏数、浏览数等）
 */
fun formatCount(count: Int): String {
    return when {
        count >= 100000 -> String.format(Locale.US, "%.1fw", count / 10000.0)
        count >= 10000 -> String.format(Locale.US, "%.1f万", count / 10000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        else -> count.toString()
    }
}

/**
 * 格式化日期显示
 */
fun formatDate(dateStr: String): String? {
    return try {
        val zonedDateTime = ZonedDateTime.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        null
    }
}

/**
 * 格式化数字显示（用于用户统计数据）
 */
fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format(Locale.US, "%.1fw", num / 10000.0)
        num >= 1000 -> String.format(Locale.US, "%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
