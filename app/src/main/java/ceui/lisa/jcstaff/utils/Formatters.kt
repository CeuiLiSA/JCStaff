package ceui.lisa.jcstaff.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ceui.lisa.jcstaff.R
import java.time.Duration
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
 * 格式化发布日期为相对时间（支持国际化）
 * - < 1 分钟：刚刚
 * - < 1 小时：X 分钟前
 * - < 1 天：X 小时前
 * - < 30 天：X 天前
 * - >= 30 天：本地化的绝对日期时间
 */
@Composable
fun formatRelativeDate(dateStr: String): String? {
    val pattern = stringResource(R.string.date_format_absolute)
    val zonedDateTime = ZonedDateTime.parse(dateStr)
    val now = ZonedDateTime.now()
    val duration = Duration.between(zonedDateTime, now)

    if (duration.isNegative) {
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()

    return when {
        minutes < 1 -> stringResource(R.string.just_now)
        hours < 1 -> stringResource(R.string.minutes_ago, minutes)
        days < 1 -> stringResource(R.string.hours_ago, hours)
        days < 30 -> stringResource(R.string.days_ago, days)
        else -> {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            zonedDateTime.format(formatter)
        }
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
