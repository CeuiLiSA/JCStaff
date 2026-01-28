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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.MetaInfoRow
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.utils.formatRelativeDate

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
            val formattedDate = formatRelativeDate(dateStr)
            if (formattedDate != null) {
                MetaInfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.publish_date),
                    value = formattedDate
                )
            }
        }

        // 图片尺寸
        if (illust.width > 0 && illust.height > 0) {
            MetaInfoRow(
                icon = Icons.Default.Photo,
                label = stringResource(R.string.size),
                value = "${illust.width} × ${illust.height}"
            )
        }

        // 页数
        if (illust.page_count > 1) {
            MetaInfoRow(
                icon = Icons.Default.PhotoLibrary,
                label = stringResource(R.string.page_count),
                value = "${illust.page_count} ${stringResource(R.string.page_unit)}"
            )
        }

        // 作品类型
        val typeText = when {
            illust.isGif() -> stringResource(R.string.type_ugoira)
            illust.isManga() -> stringResource(R.string.type_manga)
            else -> stringResource(R.string.type_illust)
        }
        MetaInfoRow(
            icon = when {
                illust.isGif() -> Icons.Default.PlayCircle
                illust.isManga() -> Icons.Default.PhotoLibrary
                else -> Icons.Default.Image
            },
            label = stringResource(R.string.type),
            value = typeText
        )

        // AI 类型
        if (illust.illust_ai_type > 0) {
            val aiText = when (illust.illust_ai_type) {
                1 -> stringResource(R.string.ai_assisted)
                2 -> stringResource(R.string.ai_generated)
                else -> stringResource(R.string.ai_related)
            }
            MetaInfoRow(
                icon = Icons.Default.AutoAwesome,
                label = stringResource(R.string.ai),
                value = aiText,
                valueColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
