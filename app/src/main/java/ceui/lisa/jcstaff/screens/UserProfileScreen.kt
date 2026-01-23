package ceui.lisa.jcstaff.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserProfile
import ceui.lisa.jcstaff.network.Workspace
import ceui.lisa.jcstaff.profile.UserProfileViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    userId: Long,
    onBackClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    viewModel: UserProfileViewModel = viewModel(key = "user_profile_$userId")
) {
    val state by viewModel.state.collectAsState()
    val selectionManager = rememberSelectionManager()
    val context = LocalContext.current

    // 更多菜单状态
    var showMoreMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val horizontalPadding = if (gridSpacingEnabled) 8.dp else 0.dp

    val gridState = rememberPersistentLazyStaggeredGridState("user_profile_$userId")

    LaunchedEffect(gridState, state.canLoadMore) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 4
        }
            .distinctUntilChanged()
            .filter { it && state.canLoadMore }
            .collect {
                viewModel.loadMore()
            }
    }

    Box {
        PullToRefreshBox(
            isRefreshing = state.isLoadingProfile && state.user != null,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 0.dp,
                    bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
                modifier = Modifier.fillMaxSize()
            ) {
                // 沉浸式头部
                item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                    UserProfileHeader(
                        user = state.user,
                        profile = state.profile,
                        workspace = state.workspace,
                        isLoading = state.isLoadingProfile,
                        isFollowing = state.isFollowing,
                        onFollowClick = { viewModel.toggleFollow() }
                    )
                }

                // 加载中状态
                if (state.isLoadingIllusts && state.illusts.isEmpty()) {
                    item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // 错误状态
                if (state.illustsError != null && state.illusts.isEmpty()) {
                    item(key = "error", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.illustsError ?: "加载失败",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 空状态
                if (!state.isLoadingIllusts && state.illusts.isEmpty() && state.illustsError == null && state.user != null) {
                    item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无作品",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 作品列表
                items(state.illusts, key = { it.id }) { illust ->
                    IllustCard(
                        illust = illust,
                        onClick = { onIllustClick(illust) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        isSelectionMode = selectionManager.isSelectionMode,
                        isSelected = selectionManager.isSelected(illust.id),
                        onLongPress = { selectionManager.onLongPress(illust) },
                        onSelectionToggle = { selectionManager.toggleSelection(illust) },
                        showIllustInfo = showIllustInfo,
                        cornerRadius = illustCornerRadius
                    )
                }

                // 加载更多指示器
                if (state.isLoadingMore) {
                    item(key = "loading_more", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // 顶部阴影渐变
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding.calculateTopPadding() + 60.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 浮动顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 4.dp,
                    start = 4.dp,
                    end = 4.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            // 右侧按钮组
            Row {
                // 分享按钮
                IconButton(
                    onClick = {
                        val shareUrl = "https://www.pixiv.net/users/$userId"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                            putExtra(Intent.EXTRA_TITLE, state.user?.name ?: "")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "分享用户"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White
                    )
                }

                // 更多按钮
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("举报") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("屏蔽") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }

        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}

@Composable
private fun UserProfileHeader(
    user: User?,
    profile: UserProfile?,
    workspace: Workspace?,
    isLoading: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // 背景图 + 头像区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // 背景图或渐变色
            if (profile?.background_image_url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profile.background_image_url)
                        .crossfade(true)
                        .addHeader("Referer", "https://app-api.pixiv.net/")
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            // 头像（底部居中，向下偏移）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 48.dp)
            ) {
                if (isLoading && user == null) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user?.profile_image_urls?.medium)
                            .crossfade(true)
                            .addHeader("Referer", "https://app-api.pixiv.net/")
                            .build(),
                        contentDescription = user?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    // Premium 徽章
                    if (user?.is_premium == true || profile?.is_premium == true) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Premium",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 头像下方空间
        Spacer(modifier = Modifier.height(56.dp))

        // 用户名和账号
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = user?.name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "@${user?.account ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 统计数据
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = profile?.total_illusts ?: 0,
                label = "插画"
            )
            StatItem(
                value = profile?.total_manga ?: 0,
                label = "漫画"
            )
            StatItem(
                value = profile?.total_follow_users ?: 0,
                label = "关注"
            )
            StatItem(
                value = profile?.total_illust_bookmarks_public ?: 0,
                label = "收藏"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 关注按钮
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isFollowed = user?.is_followed == true
            if (isFollowed) {
                OutlinedButton(
                    onClick = onFollowClick,
                    enabled = !isFollowing && user != null,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    if (isFollowing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("已关注")
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onFollowClick,
                    enabled = !isFollowing && user != null,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    if (isFollowing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关注")
                    }
                }
            }
        }

        // 简介
        if (!user?.comment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = user?.comment ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // 用户详细信息
        val hasDetailInfo = !profile?.job.isNullOrBlank() ||
                !profile?.region.isNullOrBlank() ||
                profile?.birth_day != null ||
                !profile?.webpage.isNullOrBlank() ||
                !profile?.twitter_account.isNullOrBlank()

        if (hasDetailInfo) {
            Spacer(modifier = Modifier.height(20.dp))

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
                    // 职业
                    if (!profile?.job.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.Work,
                            label = "职业",
                            value = profile?.job ?: ""
                        )
                    }

                    // 地区
                    if (!profile?.region.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "地区",
                            value = profile?.region ?: ""
                        )
                    }

                    // 生日
                    if (profile?.birth_day != null) {
                        val birthText = if (profile.birth_year != null && profile.birth_year > 0) {
                            "${profile.birth_year}年${profile.birth_day}"
                        } else {
                            profile.birth_day
                        }
                        InfoRow(
                            icon = Icons.Default.Cake,
                            label = "生日",
                            value = birthText
                        )
                    }

                    // 网站
                    if (!profile?.webpage.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.Language,
                            label = "网站",
                            value = profile?.webpage ?: "",
                            isLink = true,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profile?.webpage))
                                context.startActivity(intent)
                            }
                        )
                    }

                    // Twitter
                    if (!profile?.twitter_account.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.Language,
                            label = "Twitter",
                            value = "@${profile?.twitter_account}",
                            isLink = true,
                            onClick = {
                                val url = profile?.twitter_url ?: "https://twitter.com/${profile?.twitter_account}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // 工作环境信息
        val hasWorkspaceInfo = !workspace?.pc.isNullOrBlank() ||
                !workspace?.monitor.isNullOrBlank() ||
                !workspace?.tool.isNullOrBlank() ||
                !workspace?.tablet.isNullOrBlank() ||
                !workspace?.mouse.isNullOrBlank() ||
                !workspace?.desk.isNullOrBlank() ||
                !workspace?.chair.isNullOrBlank() ||
                !workspace?.music.isNullOrBlank() ||
                !workspace?.comment.isNullOrBlank()

        if (hasWorkspaceInfo) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "工作环境",
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
                        WorkspaceRow(label = "电脑", value = workspace?.pc ?: "")
                    }
                    if (!workspace?.monitor.isNullOrBlank()) {
                        WorkspaceRow(label = "显示器", value = workspace?.monitor ?: "")
                    }
                    if (!workspace?.tool.isNullOrBlank()) {
                        WorkspaceRow(label = "软件", value = workspace?.tool ?: "")
                    }
                    if (!workspace?.tablet.isNullOrBlank()) {
                        WorkspaceRow(label = "数位板", value = workspace?.tablet ?: "")
                    }
                    if (!workspace?.mouse.isNullOrBlank()) {
                        WorkspaceRow(label = "鼠标", value = workspace?.mouse ?: "")
                    }
                    if (!workspace?.desk.isNullOrBlank()) {
                        WorkspaceRow(label = "桌子", value = workspace?.desk ?: "")
                    }
                    if (!workspace?.chair.isNullOrBlank()) {
                        WorkspaceRow(label = "椅子", value = workspace?.chair ?: "")
                    }
                    if (!workspace?.music.isNullOrBlank()) {
                        WorkspaceRow(label = "音乐", value = workspace?.music ?: "")
                    }
                    if (!workspace?.comment.isNullOrBlank()) {
                        WorkspaceRow(label = "备注", value = workspace?.comment ?: "")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 作品分隔线
        if (user != null) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "作品",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    value: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WorkspaceRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format(Locale.US, "%.1fw", num / 10000.0)
        num >= 1000 -> String.format(Locale.US, "%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
