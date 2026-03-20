package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.core.AppLanguage
import ceui.lisa.jcstaff.core.FilenameFormatter
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.User
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: User?,
    onLogoutClick: () -> Unit = {}
) {
    val navViewModel = LocalNavigationViewModel.current
    val imageCacheLimitMb by SettingsStore.imageCacheLimitMb.collectAsState()
    val hideAiContent by SettingsStore.hideAiContent.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val filenameTemplate by SettingsStore.downloadFilenameTemplate.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFilenameTemplateDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // ==================== 账号 ====================
            SettingsSectionHeader(title = stringResource(R.string.settings_account))

            // ==================== 用户信息卡片 ====================
            UserProfileCard(
                user = currentUser,
                onClick = {
                    currentUser?.let { user ->
                        navViewModel.navigate(NavRoute.UserProfile(userId = user.id))
                    }
                }
            )


            // 账号管理
            SettingsItemNavigation(
                icon = Icons.Default.SwitchAccount,
                title = stringResource(R.string.account_management),
                description = stringResource(R.string.account_management_desc),
                onClick = { navViewModel.navigate(NavRoute.AccountManagement) }
            )

            // 退出登录
            SettingsItemDanger(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.logout),
                description = stringResource(R.string.logout_desc),
                onClick = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsDivider()

            // ==================== 通用设置 ====================
            SettingsSectionHeader(title = stringResource(R.string.settings_general))

            // 语言
            SettingsItemNavigation(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language_title),
                description = stringResource(R.string.language_desc),
                value = currentLanguage?.displayName ?: "",
                onClick = { showLanguageDialog = true }
            )

            // 屏蔽设置
            SettingsItemNavigation(
                icon = Icons.Default.Block,
                title = stringResource(R.string.block_settings),
                description = stringResource(R.string.block_settings_desc),
                onClick = { navViewModel.navigate(NavRoute.BlockSettings) }
            )

            // 隐藏AI作品
            SettingsItemSwitch(
                icon = Icons.Default.SmartToy,
                title = stringResource(R.string.hide_ai_content_title),
                description = stringResource(R.string.hide_ai_content_desc),
                checked = hideAiContent,
                onCheckedChange = {
                    coroutineScope.launch {
                        SettingsStore.setHideAiContent(it)
                    }
                }
            )

            SettingsDivider()

            // ==================== 存储 ====================
            SettingsSectionHeader(title = stringResource(R.string.settings_storage))

            // 缓存管理
            SettingsItemNavigation(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.cache_browser),
                description = stringResource(R.string.cache_browser_desc),
                onClick = { navViewModel.navigate(NavRoute.CacheBrowser()) }
            )

            // 图片缓存上限
            SettingsItemSlider(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.image_cache_limit_title),
                description = stringResource(R.string.image_cache_limit_desc),
                value = imageCacheLimitMb.toFloat(),
                valueLabel = if (imageCacheLimitMb >= 1024) {
                    "${"%.1f".format(imageCacheLimitMb / 1024f)}GB"
                } else {
                    "${imageCacheLimitMb}MB"
                },
                valueRange = SettingsStore.MIN_IMAGE_CACHE_LIMIT_MB.toFloat()..SettingsStore.MAX_IMAGE_CACHE_LIMIT_MB.toFloat(),
                steps = (SettingsStore.MAX_IMAGE_CACHE_LIMIT_MB - SettingsStore.MIN_IMAGE_CACHE_LIMIT_MB) / 256 - 1,
                onValueChange = { value ->
                    coroutineScope.launch {
                        SettingsStore.setImageCacheLimitMb(value.roundToInt())
                    }
                }
            )

            // 下载文件名格式
            SettingsItemNavigation(
                icon = Icons.Default.TextFields,
                title = stringResource(R.string.filename_template_title),
                description = stringResource(R.string.filename_template_desc),
                onClick = { showFilenameTemplateDialog = true }
            )

            SettingsDivider()

            // 关于
            SettingsItemNavigation(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                description = stringResource(R.string.settings_about_desc),
                onClick = { navViewModel.navigate(NavRoute.About) }
            )

            // 底部安全区域
            val navBarBottom =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Spacer(modifier = Modifier.height(16.dp + navBarBottom))
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage ?: AppLanguage.ENGLISH,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { language ->
                showLanguageDialog = false
                coroutineScope.launch {
                    LanguageManager.setLanguage(language)
                }
            }
        )
    }

    // 文件名模板编辑对话框
    if (showFilenameTemplateDialog) {
        FilenameTemplateDialog(
            currentTemplate = filenameTemplate,
            onDismiss = { showFilenameTemplateDialog = false },
            onConfirm = { newTemplate ->
                showFilenameTemplateDialog = false
                coroutineScope.launch {
                    SettingsStore.setDownloadFilenameTemplate(newTemplate)
                }
            }
        )
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogoutClick()
            }
        )
    }
}

// ==================== 用户信息卡片 ====================

@Composable
private fun UserProfileCard(
    user: User?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        CircleAvatar(
            imageUrl = user?.profile_image_urls?.findAvatarUrl(),
            size = 64.dp,
            contentDescription = user?.name,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.name ?: stringResource(R.string.not_logged_in),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (user?.account != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${user.account}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.view_profile),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 设置项组件 ====================

@Composable
private fun SettingsSectionHeader(title: String) {
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
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsItemSwitch(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsItemSlider(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp, top = 4.dp)
        )
    }
}

@Composable
private fun SettingsItemNavigation(
    icon: ImageVector,
    title: String,
    description: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsItemDanger(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== 对话框 ====================

@Composable
private fun LanguageDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (AppLanguage) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (language == selectedLanguage),
                                onClick = { selectedLanguage = language },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language == selectedLanguage),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedLanguage) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.logout_confirm_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.logout_confirm_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.logout),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FilenameTemplateDialog(
    currentTemplate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var templateText by remember { mutableStateOf(currentTemplate) }

    // Preview using a sample Illust
    val previewIllust = remember {
        Illust(
            id = 142141392,
            title = "Sample Title",
            create_date = "2025-01-15T12:00:00+09:00",
            user = User(
                id = 12345678,
                name = "Artist"
            )
        )
    }
    val previewFilename = remember(templateText) {
        val effective = templateText.ifBlank { SettingsStore.DEFAULT_FILENAME_TEMPLATE }
        FilenameFormatter.format(effective, previewIllust, pageIndex = 0) + ".jpg"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.filename_template_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = templateText,
                    onValueChange = { templateText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview
                Text(
                    text = stringResource(R.string.filename_template_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = previewFilename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Available variables
                Text(
                    text = stringResource(R.string.filename_template_variables),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reset to default
                TextButton(
                    onClick = { templateText = SettingsStore.DEFAULT_FILENAME_TEMPLATE }
                ) {
                    Text(stringResource(R.string.filename_template_reset))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(templateText) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

