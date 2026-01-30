package ceui.lisa.jcstaff.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 操作菜单项
 */
data class ActionMenuItem(
    val icon: ImageVector,
    val label: String,
    val color: Color? = null,
    val onClick: () -> Unit
)

/**
 * 通用操作菜单弹窗
 *
 * 居中显示，无多余边距
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionMenu(
    onDismiss: () -> Unit,
    items: List<ActionMenuItem>
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column {
                items.forEach { item ->
                    val itemColor = item.color ?: MaterialTheme.colorScheme.onSurface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = itemColor
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = itemColor
                        )
                    }
                }
            }
        }
    }
}
