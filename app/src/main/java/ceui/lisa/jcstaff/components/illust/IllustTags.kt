package ceui.lisa.jcstaff.components.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.Tag

// 16 组优雅的标签渐变色
private val tagGradients = listOf(
    // 经典优雅系列
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),  // 紫蓝梦幻
    listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),  // 珊瑚日落
    listOf(Color(0xFF4ECDC4), Color(0xFF44A08D)),  // 薄荷青翠
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),  // 樱花粉红
    listOf(Color(0xFF5EE7DF), Color(0xFFB490CA)),  // 极光紫青
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),  // 晚霞金粉

    // 高级灰调系列
    listOf(Color(0xFF8E9EAB), Color(0xFFEEF2F3)),  // 银灰轻雾
    listOf(Color(0xFF3A6186), Color(0xFF89253E)),  // 深邃酒红

    // 自然系列
    listOf(Color(0xFF56AB2F), Color(0xFFA8E063)),  // 森林新绿
    listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)),  // 海洋蔚蓝
    listOf(Color(0xFFCC2B5E), Color(0xFF753A88)),  // 紫罗兰夜
    listOf(Color(0xFFED4264), Color(0xFFFFEDBC)),  // 黄昏暖阳

    // 现代科技系列
    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),  // 赛博青绿
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),  // 电子蓝紫
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),  // 霓虹玫红
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),  // 琥珀金黄
)

/**
 * 作品标签组件 - 升级版
 * 渐变背景 + 玻璃边框 + 按压缩放动画 + 径向光晕
 */
@Composable
fun IllustTags(
    tags: List<Tag>,
    onTagClick: ((Tag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.tags),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEachIndexed { index, tag ->
                val gradientIndex = index % tagGradients.size
                FancyTagChip(
                    tag = tag,
                    gradientColors = tagGradients[gradientIndex],
                    onClick = { onTagClick?.invoke(tag) }
                )
            }
        }
    }
}

/**
 * 高级标签芯片
 * 微妙渐变背景 + 玻璃边框效果
 */
@Composable
private fun FancyTagChip(
    tag: Tag,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val cornerRadius = 8.dp

    Column(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        gradientColors[0].copy(alpha = 0.15f),
                        gradientColors[1].copy(alpha = 0.1f)
                    )
                )
            )
            .drawBehind {
                // 玻璃边框效果（白色半透明边框）
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "#${tag.name ?: ""}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = gradientColors[0],
            maxLines = 1
        )
        tag.translated_name?.let { translated ->
            Text(
                text = translated,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}
