package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.auth.AccountRegistry
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.components.FloatingTopBar
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.IllustScrollAwareTopBar
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.components.comment.CommentPreviewSection
import ceui.lisa.jcstaff.components.illust.CollapsibleImageSection
import ceui.lisa.jcstaff.components.illust.IllustActionBar
import ceui.lisa.jcstaff.components.illust.IllustAuthorRow
import ceui.lisa.jcstaff.components.illust.IllustCaption
import ceui.lisa.jcstaff.components.illust.IllustMetaInfo
import ceui.lisa.jcstaff.components.illust.IllustTags
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustDetailScreen(
    illustId: Long,
    title: String,
    previewUrl: String,
    aspectRatio: Float,
    detailViewModel: IllustDetailViewModel = viewModel(key = "detail_$illustId"),
    relatedViewModel: IllustListViewModel = viewModel(key = "related_$illustId")
) {
    val navViewModel = LocalNavigationViewModel.current
    val currentUserId by AccountRegistry.activeUserId.collectAsState(initial = null)

    // 从 ViewModel 获取状态
    val detailState by detailViewModel.state.collectAsState()
    val illust = detailState.illust
    val isLoading = detailState.isLoading
    val error = detailState.error
    val isBookmarked = detailState.isBookmarked

    // 绑定 ViewModel
    LaunchedEffect(illustId) {
        detailViewModel.bind(illustId)
    }

    // 关注状态：直接观察 ObjectStore 中的 User，确保跨页面同步
    val userId = illust?.user?.id
    val observedUser by remember(userId) {
        userId?.let { detailViewModel.getUserFollowState(it) }
    }?.collectAsState() ?: remember { mutableStateOf(null) }
    val isFollowed = observedUser?.is_followed ?: illust?.user?.is_followed ?: false

    // 图片区域展开/收起状态
    var isImagesExpanded by remember { mutableStateOf(false) }

    // 选择模式
    val selectionManager = LocalSelectionManager.current

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 相关作品状态
    val relatedState by relatedViewModel.state.collectAsState()

    // 绑定相关作品加载器
    LaunchedEffect(illustId) {
        relatedViewModel.bind(IllustLoader {
            PixivClient.pixivApi.getRelatedIllusts(illustId)
        })
    }

    val firstOriginalUrl = remember(illust) {
        detailViewModel.getFirstOriginalUrl()
    }

    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()

    // 滚动感知 TopBar 显示状态 - 使用 derivedStateOf 避免返回页面时触发动画
    val showScrollAwareTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 1200
        }
    }

    Box {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            val showIllustInfo by SettingsStore.showIllustInfo.collectAsState()
            val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState()
            val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState()
            val gridSpacing = if (gridSpacingEnabled) 8.dp else 1.dp

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = if (gridSpacingEnabled) 8.dp else 0.dp,
                    end = if (gridSpacingEnabled) 8.dp else 0.dp,
                    top = 0.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalItemSpacing = gridSpacing,
                modifier = Modifier.fillMaxSize()
            ) {
                // 图片区域 - 可折叠
                item(key = "images_section", span = StaggeredGridItemSpan.FullLine) {
                    CollapsibleImageSection(
                        illustId = illustId,
                        title = title,
                        previewUrl = previewUrl,
                        aspectRatio = aspectRatio,
                        illust = illust,
                        isExpanded = isImagesExpanded,
                        onExpandToggle = { isImagesExpanded = !isImagesExpanded },
                        onImageClick = { clickedPreviewUrl, originalUrl, _ ->
                            navViewModel.navigate(
                                NavRoute.ImageViewer(
                                    imageUrl = clickedPreviewUrl,
                                    originalUrl = originalUrl,
                                    sharedElementKey = ""
                                )
                            )
                        }
                    )
                }

                // 加载状态
                if (isLoading) {
                    item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                } else if (error != null && illust == null) {
                    item(key = "error", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error ?: stringResource(R.string.load_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 详情内容
                illust?.let { loadedIllust ->
                    // 标题
                    item(key = "title", span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = loadedIllust.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    // 作者信息区域
                    item(key = "author", span = StaggeredGridItemSpan.FullLine) {
                        IllustAuthorRow(
                            user = loadedIllust.user,
                            isFollowed = isFollowed,
                            onFollowStateChanged = { },
                            onUserClick = { userId ->
                                navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                            }
                        )
                    }

                    // 操作按钮行
                    item(key = "actions", span = StaggeredGridItemSpan.FullLine) {
                        IllustActionBar(
                            illust = loadedIllust,
                            isBookmarked = isBookmarked,
                            downloadUrl = firstOriginalUrl ?: previewUrl,
                            onBookmarkStateChanged = { newState, updatedIllust ->
                                detailViewModel.updateBookmarkState(newState, updatedIllust)
                            },
                            currentUserId = currentUserId ?: 0L
                        )
                    }

                    // 标签
                    if (!loadedIllust.tags.isNullOrEmpty()) {
                        item(key = "tags", span = StaggeredGridItemSpan.FullLine) {
                            IllustTags(
                                tags = loadedIllust.tags,
                                onTagClick = { tag ->
                                    BrowseHistoryRepository.recordSearch(tag)
                                    navViewModel.navigate(NavRoute.TagDetail(tag = tag))
                                }
                            )
                        }
                    }

                    // 作品简介
                    if (!loadedIllust.caption.isNullOrBlank()) {
                        item(key = "caption", span = StaggeredGridItemSpan.FullLine) {
                            IllustCaption(caption = loadedIllust.caption)
                        }
                    }


                    // 元信息区域
                    item(key = "meta_info", span = StaggeredGridItemSpan.FullLine) {
                        IllustMetaInfo(illust = loadedIllust)
                    }

                    // 评论预览
                    item(key = "comment_preview", span = StaggeredGridItemSpan.FullLine) {
                        CommentPreviewSection(
                            objectId = illustId,
                            objectType = "illust",
                            onViewAll = {
                                navViewModel.navigate(
                                    NavRoute.CommentDetail(
                                        objectId = illustId,
                                        objectType = "illust"
                                    )
                                )
                            }
                        )
                    }
                }

                // 相关作品标题
                if (relatedState.illusts.isNotEmpty() || relatedState.isLoading) {
                    item(key = "related_header", span = StaggeredGridItemSpan.FullLine) {
                        RelatedIllustsHeader()
                    }
                }

                // 相关作品加载中
                if (relatedState.isLoading && relatedState.isEmpty) {
                    item(key = "related_loading", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }

                // 相关作品瀑布流
                items(relatedState.illusts, key = { "related_${it.id}" }) { relatedIllust ->
                    IllustCard(
                        illust = relatedIllust,
                        onClick = {
                            navViewModel.navigate(
                                NavRoute.IllustDetail(
                                    illustId = relatedIllust.id,
                                    title = relatedIllust.title ?: "",
                                    previewUrl = relatedIllust.previewUrl(),
                                    aspectRatio = relatedIllust.aspectRatio()
                                )
                            )
                        },
                        showIllustInfo = showIllustInfo,
                        cornerRadius = illustCornerRadius
                    )
                }

                // 加载更多指示器
                if (relatedState.isLoadingMore) {
                    item(key = "related_loading_more", span = StaggeredGridItemSpan.FullLine) {
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

            // 检测是否滚动到底部
            LaunchedEffect(gridState, relatedState.canLoadMore) {
                snapshotFlow {
                    val layoutInfo = gridState.layoutInfo
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    totalItems > 0 && lastVisibleItem >= totalItems - 4
                }
                    .distinctUntilChanged()
                    .filter { it && relatedState.canLoadMore }
                    .collect {
                        relatedViewModel.loadMore()
                    }
            }
        }

        // 浮动顶部栏 - 只在 ScrollAwareTopBar 不显示时展示
        if (!showScrollAwareTopBar) {
            FloatingTopBar(
                shareUrl = "https://www.pixiv.net/artworks/$illustId",
                shareTitle = title
            )
        }

        // 滚动感知 TopBar - 滚动到一定位置时显示
        IllustScrollAwareTopBar(
            illust = illust,
            isVisible = showScrollAwareTopBar,
            onScrollToTop = {
                coroutineScope.launch {
                    gridState.animateScrollToItem(0)
                }
            }
        )

        // Selection top bar overlay
        SelectionTopBar(allIllusts = relatedState.illusts)
    }
}

/**
 * 相关作品标题栏
 */
@Composable
private fun RelatedIllustsHeader(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(top = 24.dp)) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.related_works),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}
