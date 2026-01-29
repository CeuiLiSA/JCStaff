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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.cache.BrowseHistoryManager
import ceui.lisa.jcstaff.components.FloatingTopBar
import ceui.lisa.jcstaff.components.IllustCard
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
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustDetailScreen(
    illustId: Long,
    title: String,
    previewUrl: String,
    aspectRatio: Float,
    relatedViewModel: IllustListViewModel = viewModel(key = "related_$illustId")
) {
    val navViewModel = LocalNavigationViewModel.current
    // 从 ObjectStore 获取缓存数据
    val cachedIllust = remember(illustId) {
        ObjectStore.peek<Illust>(StoreKey(illustId, StoreType.ILLUST))
    }

    val illustFlow = remember(illustId) {
        ObjectStore.get<Illust>(StoreKey(illustId, StoreType.ILLUST))
    }
    val observedIllust by illustFlow?.collectAsState() ?: remember { mutableStateOf(cachedIllust) }

    var illust by remember { mutableStateOf(cachedIllust) }
    var isLoading by remember { mutableStateOf(cachedIllust == null) }
    var error by remember { mutableStateOf<String?>(null) }

    // 收藏状态
    var isBookmarked by remember(illustId) { mutableStateOf(cachedIllust?.is_bookmarked ?: false) }

    // 关注状态
    var isFollowed by remember(illustId) {
        mutableStateOf(
            cachedIllust?.user?.is_followed ?: false
        )
    }

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

    LaunchedEffect(observedIllust) {
        observedIllust?.let {
            illust = it
            isBookmarked = it.is_bookmarked ?: false
            isFollowed = it.user?.is_followed ?: false
        }
    }

    // 加载作品详情
    LaunchedEffect(illustId) {
        if (cachedIllust == null) {
            isLoading = true
            error = null
            try {
                val response = PixivClient.pixivApi.getIllustDetail(illustId)
                response.illust?.let { fetchedIllust ->
                    ObjectStore.put(fetchedIllust)
                    fetchedIllust.user?.let { user -> ObjectStore.put(user) }
                    illust = fetchedIllust
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // 绑定相关作品加载器
    LaunchedEffect(illustId) {
        relatedViewModel.bind(IllustLoader {
            PixivClient.pixivApi.getRelatedIllusts(illustId)
        })
    }

    // 记录浏览历史
    LaunchedEffect(illust) {
        illust?.let { BrowseHistoryManager.recordView(it) }
    }

    val firstOriginalUrl = remember(illust) {
        illust?.let { loadedIllust ->
            if (loadedIllust.page_count == 1) {
                loadedIllust.meta_single_page?.original_image_url
            } else {
                loadedIllust.meta_pages?.firstOrNull()?.image_urls?.original
            }
        }
    }

    Box {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            val gridState = rememberLazyStaggeredGridState()
            val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
            val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 0.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 4.dp,
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
                            navViewModel.navigate(NavRoute.ImageViewer(
                                imageUrl = clickedPreviewUrl,
                                originalUrl = originalUrl,
                                sharedElementKey = ""
                            ))
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
                            CircularProgressIndicator()
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
                            onFollowStateChanged = { followed -> isFollowed = followed },
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
                                isBookmarked = newState
                                illust = updatedIllust
                            }
                        )
                    }

                    // 标签
                    if (!loadedIllust.tags.isNullOrEmpty()) {
                        item(key = "tags", span = StaggeredGridItemSpan.FullLine) {
                            IllustTags(
                                tags = loadedIllust.tags,
                                onTagClick = { tag ->
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
                                navViewModel.navigate(NavRoute.CommentDetail(
                                    objectId = illustId,
                                    objectType = "illust"
                                ))
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
                            CircularProgressIndicator()
                        }
                    }
                }

                // 相关作品瀑布流
                items(relatedState.illusts, key = { "related_${it.id}" }) { relatedIllust ->
                    IllustCard(
                        illust = relatedIllust,
                        onClick = {
                            navViewModel.navigate(NavRoute.IllustDetail(
                                illustId = relatedIllust.id,
                                title = relatedIllust.title ?: "",
                                previewUrl = relatedIllust.previewUrl(),
                                aspectRatio = relatedIllust.aspectRatio()
                            ))
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

        // 浮动顶部栏
        FloatingTopBar(
            shareUrl = "https://www.pixiv.net/artworks/$illustId",
            shareTitle = title
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
