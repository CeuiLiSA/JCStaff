package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSettingsScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val blockedUserIds by ContentFilterManager.blockedUserIds.collectAsState()
    val blockedContentIds by ContentFilterManager.blockedContentIds.collectAsState()
    val blockedTags by ContentFilterManager.blockedTags.collectAsState()

    val hasAnyBlocked = blockedUserIds.isNotEmpty() ||
            blockedContentIds.isNotEmpty() ||
            blockedTags.isNotEmpty()

    // 取消屏蔽确认弹窗状态
    var pendingUnblock by remember { mutableStateOf<UnblockAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.block_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasAnyBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_blocked_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ===== 已屏蔽的用户 =====
                if (blockedUserIds.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.blocked_users))
                    }
                    items(blockedUserIds.toList()) { userId ->
                        BlockedItem(
                            icon = Icons.Default.Person,
                            title = stringResource(R.string.user_id_label, userId),
                            onUnblock = {
                                pendingUnblock = UnblockAction.User(userId)
                            }
                        )
                    }
                    item { SectionDivider() }
                }

                // ===== 已屏蔽的作品 =====
                if (blockedContentIds.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.blocked_works))
                    }
                    items(blockedContentIds.toList()) { contentId ->
                        BlockedItem(
                            icon = Icons.Default.HideSource,
                            title = stringResource(R.string.work_id_label, contentId),
                            onUnblock = {
                                pendingUnblock = UnblockAction.Content(contentId)
                            }
                        )
                    }
                    item { SectionDivider() }
                }

                // ===== 已屏蔽的标签 =====
                if (blockedTags.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.blocked_tags))
                    }
                    items(blockedTags.toList()) { tag ->
                        BlockedItem(
                            icon = Icons.Default.Tag,
                            title = tag,
                            onUnblock = {
                                pendingUnblock = UnblockAction.Tag(tag)
                            }
                        )
                    }
                }

                // 底部安全区域
                item {
                    val navBarBottom =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Spacer(modifier = Modifier.height(16.dp + navBarBottom))
                }
            }
        }
    }

    // 取消屏蔽确认弹窗
    pendingUnblock?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingUnblock = null },
            title = { Text(stringResource(R.string.unblock_confirm_title)) },
            text = { Text(stringResource(R.string.unblock_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    action.execute()
                    pendingUnblock = null
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnblock = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** 待执行的取消屏蔽动作 */
private sealed class UnblockAction {
    abstract fun execute()

    data class User(val userId: Long) : UnblockAction() {
        override fun execute() = ContentFilterManager.unblockUser(userId)
    }

    data class Content(val contentId: Long) : UnblockAction() {
        override fun execute() = ContentFilterManager.unblockContent(contentId)
    }

    data class Tag(val tag: String) : UnblockAction() {
        override fun execute() = ContentFilterManager.unblockTag(tag)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun BlockedItem(
    icon: ImageVector,
    title: String,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        OutlinedButton(onClick = onUnblock) {
            Text(stringResource(R.string.unblock))
        }
    }
}
