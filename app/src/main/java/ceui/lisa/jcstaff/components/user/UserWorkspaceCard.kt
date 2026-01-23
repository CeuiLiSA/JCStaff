package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.Workspace

/**
 * 工作环境信息卡片组件
 * 显示用户的工作环境信息
 */
@Composable
fun UserWorkspaceCard(
    workspace: Workspace?,
    modifier: Modifier = Modifier
) {
    val hasWorkspaceInfo = !workspace?.pc.isNullOrBlank() ||
            !workspace?.monitor.isNullOrBlank() ||
            !workspace?.tool.isNullOrBlank() ||
            !workspace?.tablet.isNullOrBlank() ||
            !workspace?.mouse.isNullOrBlank() ||
            !workspace?.desk.isNullOrBlank() ||
            !workspace?.chair.isNullOrBlank() ||
            !workspace?.music.isNullOrBlank() ||
            !workspace?.comment.isNullOrBlank()

    if (!hasWorkspaceInfo) return

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.workspace),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!workspace?.pc.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_pc), value = workspace?.pc ?: "")
                }
                if (!workspace?.monitor.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_monitor), value = workspace?.monitor ?: "")
                }
                if (!workspace?.tool.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_software), value = workspace?.tool ?: "")
                }
                if (!workspace?.tablet.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_tablet), value = workspace?.tablet ?: "")
                }
                if (!workspace?.mouse.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_mouse), value = workspace?.mouse ?: "")
                }
                if (!workspace?.desk.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_desk), value = workspace?.desk ?: "")
                }
                if (!workspace?.chair.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_chair), value = workspace?.chair ?: "")
                }
                if (!workspace?.music.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_music), value = workspace?.music ?: "")
                }
                if (!workspace?.comment.isNullOrBlank()) {
                    WorkspaceRow(label = stringResource(R.string.workspace_notes), value = workspace?.comment ?: "")
                }
            }
        }
    }
}
