package ceui.lisa.jcstaff.components.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.MetaInfoRow
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.utils.formatDate

/**
 * 作品元信息组件
 * 显示发布时间、尺寸、页数、类型和AI类型
 */
@Composable
fun IllustMetaInfo(
    illust: Illust,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 发布时间
        illust.create_date?.let { dateStr ->
            val formattedDate = formatDate(dateStr)
            if (formattedDate != null) {
                MetaInfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "发布时间",
                    value = formattedDate
                )
            }
        }

        // 图片尺寸
        if (illust.width > 0 && illust.height > 0) {
            MetaInfoRow(
                icon = Icons.Default.Photo,
                label = "尺寸",
                value = "${illust.width} × ${illust.height}"
            )
        }

        // 页数
        if (illust.page_count > 1) {
            MetaInfoRow(
                icon = Icons.Default.PhotoLibrary,
                label = "页数",
                value = "${illust.page_count} 张"
            )
        }

        // 作品类型
        val typeText = when {
            illust.isGif() -> "动图 (Ugoira)"
            illust.isManga() -> "漫画"
            else -> "插画"
        }
        MetaInfoRow(
            icon = when {
                illust.isGif() -> Icons.Default.PlayCircle
                illust.isManga() -> Icons.Default.PhotoLibrary
                else -> Icons.Default.Image
            },
            label = "类型",
            value = typeText
        )

        // AI 类型
        if (illust.illust_ai_type > 0) {
            val aiText = when (illust.illust_ai_type) {
                1 -> "AI 辅助创作"
                2 -> "AI 生成"
                else -> "AI 相关"
            }
            MetaInfoRow(
                icon = Icons.Default.AutoAwesome,
                label = "AI",
                value = aiText,
                valueColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
