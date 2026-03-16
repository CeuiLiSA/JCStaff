package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ceui.lisa.jcstaff.components.BlockType
import ceui.lisa.jcstaff.components.BlockUserDialog
import ceui.lisa.jcstaff.components.BlockedContentOverlay
import ceui.lisa.jcstaff.components.FloatingTopBar
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.components.UserScrollAwareTopBar
import ceui.lisa.jcstaff.components.user.IllustPreviewRow
import ceui.lisa.jcstaff.components.user.NovelPreviewRow
import ceui.lisa.jcstaff.components.user.ProfileSectionHeader
import ceui.lisa.jcstaff.components.user.ProfileSubSection
import ceui.lisa.jcstaff.components.user.UserProfileHeader
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.profile.UserProfileViewModel
import kotlinx.coroutines.launch

private const val CONTENT_TYPE_HEADER = "profile_header"
private const val CONTENT_TYPE_SECTION_HEADER = "section_header"
private const val CONTENT_TYPE_SUB_SECTION = "sub_section"
private const val CONTENT_TYPE_SPACER = "spacer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Long,
    viewModel: UserProfileViewModel = viewModel(key = "user_profile_$userId")
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // 记录用户浏览历史
    LaunchedEffect(state.user) {
        state.user?.let { BrowseHistoryRepository.recordUser(it) }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 滚动感知 TopBar 显示状态
    val showScrollAwareTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 1200
        }
    }

    // 屏蔽确认对话框
    var showBlockDialog by remember { mutableStateOf(false) }

    // 屏蔽状态观察
    val blockedUserIds by ContentFilterManager.blockedUserIds.collectAsState()
    val isBlocked = userId in blockedUserIds

    // 预先记住回调，避免每次重组创建新 lambda
    val onFollowClick = remember { { viewModel.toggleFollow() } }
    val onPrivateFollowClick = remember { { viewModel.toggleFollow("private") } }
    val onFollowingClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.UserFollowing(userId = userId)) }
    }
    val onRefresh = remember { { viewModel.refresh() } }
    val onIllustClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.UserCreatedIllusts(userId = userId, type = "illust")) }
    }
    val onMangaClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.UserCreatedIllusts(userId = userId, type = "manga")) }
    }
    val onNovelsClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.UserCreatedNovels(userId = userId)) }
    }
    val onBookmarkedIllustsClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.Bookmarks(userId = userId)) }
    }
    val onBookmarkedNovelsClick = remember(navViewModel, userId) {
        { navViewModel.navigate(NavRoute.UserBookmarkNovels(userId = userId)) }
    }
    val onScrollToTop = remember(coroutineScope, listState) {
        {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
            Unit
        }
    }

    // 预先记住字符串资源
    val sectionCreatedTitle = stringResource(R.string.section_created)
    val sectionBookmarkedTitle = stringResource(R.string.section_bookmarked)
    val illustsTitle = stringResource(R.string.illustrations)
    val illustMangaTitle = stringResource(R.string.tab_illust_manga)
    val mangaTitle = stringResource(R.string.manga)
    val novelTitle = stringResource(R.string.tab_novel)

    // 预先记住分享 URL
    val shareUrl = remember(userId) { "https://www.pixiv.net/users/$userId" }

    BlockedContentOverlay(
        isBlocked = isBlocked,
        blockType = BlockType.USER,
        onUnblock = { ContentFilterManager.unblockUser(userId) }
    ) {
    Box {
        PullToRefreshBox(
            isRefreshing = state.isLoadingProfile && state.user != null,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 沉浸式头部 with parallax
                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    UserProfileHeader(
                        user = state.user,
                        profile = state.profile,
                        workspace = state.workspace,
                        isLoading = state.isLoadingProfile,
                        isFollowing = state.isFollowing,
                        onFollowClick = onFollowClick,
                        onPrivateFollowClick = onPrivateFollowClick,
                        onFollowingClick = onFollowingClick
                    )
                }

                // ═══════ 创作 ═══════
                item(key = "header_created", contentType = CONTENT_TYPE_SECTION_HEADER) {
                    ProfileSectionHeader(title = sectionCreatedTitle, icon = Icons.Rounded.AutoAwesome)
                }

                // 插画
                item(key = "sub_illusts", contentType = CONTENT_TYPE_SUB_SECTION) {
                    ProfileSubSection(
                        title = illustsTitle,
                        count = state.profile?.total_illusts ?: 0,
                        onClick = onIllustClick
                    ) {
                        IllustPreviewRow(illusts = state.illusts)
                    }
                }

                // 漫画
                item(key = "sub_manga", contentType = CONTENT_TYPE_SUB_SECTION) {
                    ProfileSubSection(
                        title = mangaTitle,
                        count = state.profile?.total_manga ?: 0,
                        onClick = onMangaClick
                    ) {
                        IllustPreviewRow(illusts = state.mangaList)
                    }
                }

                // 小说
                item(key = "sub_novels", contentType = CONTENT_TYPE_SUB_SECTION) {
                    ProfileSubSection(
                        title = novelTitle,
                        count = state.profile?.total_novels ?: 0,
                        onClick = onNovelsClick
                    ) {
                        NovelPreviewRow(novels = state.novels)
                    }
                }

                // ═══════ 公开收藏 ═══════
                item(key = "header_bookmarked", contentType = CONTENT_TYPE_SECTION_HEADER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileSectionHeader(title = sectionBookmarkedTitle, icon = Icons.Rounded.Bookmarks)
                }

                // 收藏插画&漫画
                item(key = "sub_bookmarked_illusts", contentType = CONTENT_TYPE_SUB_SECTION) {
                    ProfileSubSection(
                        title = illustMangaTitle,
                        count = state.profile?.total_illust_bookmarks_public ?: 0,
                        onClick = onBookmarkedIllustsClick
                    ) {
                        IllustPreviewRow(illusts = state.bookmarkedIllusts)
                    }
                }

                // 收藏小说
                item(key = "sub_bookmarked_novels", contentType = CONTENT_TYPE_SUB_SECTION) {
                    ProfileSubSection(
                        title = novelTitle,
                        count = state.bookmarkedNovels.size,
                        onClick = onBookmarkedNovelsClick
                    ) {
                        NovelPreviewRow(novels = state.bookmarkedNovels)
                    }
                }

                // Bottom spacing
                item(key = "bottom_spacer", contentType = CONTENT_TYPE_SPACER) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 检测收藏小说区域是否可见，触发懒加载
            LaunchedEffect(listState) {
                snapshotFlow {
                    listState.layoutInfo.visibleItemsInfo.any { it.key == "sub_bookmarked_novels" }
                }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect {
                        viewModel.triggerBookmarkedNovelsLoad()
                    }
            }
        }

        // 浮动顶部栏 - 只在 ScrollAwareTopBar 不显示时展示
        if (!showScrollAwareTopBar) {
            FloatingTopBar(
                shareUrl = shareUrl,
                shareTitle = state.user?.name ?: "",
                onBlockClick = { showBlockDialog = true }
            )
        }

        // 屏蔽确认对话框
        if (showBlockDialog) {
            BlockUserDialog(
                onDismiss = { showBlockDialog = false },
                onConfirm = {
                    showBlockDialog = false
                    ContentFilterManager.blockUser(userId)
                }
            )
        }

        // 滚动感知 TopBar - 滚动到一定位置时显示
        UserScrollAwareTopBar(
            user = state.user,
            isVisible = showScrollAwareTopBar,
            onScrollToTop = onScrollToTop
        )
    }
    } // BlockedContentOverlay
}
