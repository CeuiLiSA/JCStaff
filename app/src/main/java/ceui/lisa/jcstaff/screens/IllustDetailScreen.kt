package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import ceui.lisa.jcstaff.cache.BrowseHistoryManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.IllustBoundsTransform
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.ImageDownloader
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 渐进式图片加载组件
 * 使用 LoadTaskManager 自己维护 OkHttp 下载，支持：
 * - 一级详情页和二级详情页共享进度
 * - 退出再进入时续上上一个请求
 * - 下载完成后保存到缓存文件，点击下载按钮时直接使用缓存
 */
@Composable
private fun ProgressiveImage(
    previewUrl: String,
    originalUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // 使用 LoadTaskManager 管理加载任务（自己维护 OkHttp 下载）
    // registerListener 会自动启动下载任务
    val loadTaskFlow = remember(originalUrl) {
        originalUrl?.let { LoadTaskManager.registerListener(it) }
    }
    val loadTask by loadTaskFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    // 从任务状态中获取进度和加载状态
    val downloadProgress = loadTask?.progress ?: 0f
    val isTaskLoading = loadTask?.isLoading == true
    val isTaskCompleted = loadTask?.isCompleted == true
    val cachedFilePath = loadTask?.cachedFilePath

    // 页面退出时取消监听（但不取消下载任务本身）
    DisposableEffect(originalUrl) {
        onDispose {
            originalUrl?.let { LoadTaskManager.unregisterListener(it) }
        }
    }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // 预览图（始终显示作为底层）
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        // 原图（当下载完成后，从缓存文件加载）
        if (originalUrl != null && originalUrl != previewUrl && isTaskCompleted && cachedFilePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(java.io.File(cachedFilePath))
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 原图加载中的进度指示器（带百分比）
        if (originalUrl != null && originalUrl != previewUrl && isTaskLoading && !isTaskCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.Transparent
                )
                // 进度圆环
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                    trackColor = Color.Transparent
                )
                // 百分比文字
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun IllustDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    illustId: Long,
    title: String,
    previewUrl: String,
    aspectRatio: Float,
    onBackClick: () -> Unit,
    onRelatedIllustClick: ((Illust) -> Unit)? = null,
    onImageClick: ((previewUrl: String, originalUrl: String?) -> Unit)? = null,
    onUserClick: ((Long) -> Unit)? = null,
    relatedViewModel: IllustListViewModel = viewModel(key = "related_$illustId")
) {
    val context = LocalContext.current

    // 检测 shared element transition 是否正在进行
    val isTransitionActive = sharedTransitionScope.isTransitionActive

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
    var isBookmarking by remember { mutableStateOf(false) }

    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }

    // 选择模式
    val selectionManager = rememberSelectionManager()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    val coroutineScope = rememberCoroutineScope()

    // 相关作品状态
    val relatedState by relatedViewModel.state.collectAsState()

    LaunchedEffect(observedIllust) {
        observedIllust?.let {
            illust = it
            isBookmarked = it.is_bookmarked ?: false
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
                error = e.message ?: "加载失败"
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
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = illust?.title ?: title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val downloadUrl = firstOriginalUrl ?: previewUrl
                                coroutineScope.launch {
                                    isDownloading = true
                                    val fileName = "pixiv_${illustId}_${System.currentTimeMillis()}"

                                    // 优先使用缓存文件（如果已经下载过，瞬间保存）
                                    val cachedFilePath = LoadTaskManager.getCachedFilePath(downloadUrl)
                                    val result = if (cachedFilePath != null) {
                                        // 有缓存，直接从缓存保存到相册
                                        ImageDownloader.saveFromCacheToGallery(
                                            context = context,
                                            cachedFilePath = cachedFilePath,
                                            fileName = fileName
                                        )
                                    } else {
                                        // 没有缓存，需要重新下载
                                        ImageDownloader.downloadToGallery(
                                            context = context,
                                            imageUrl = downloadUrl,
                                            fileName = fileName
                                        )
                                    }

                                    isDownloading = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isDownloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "下载原图"
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
        val gridState = rememberPersistentLazyStaggeredGridState("illust_detail_$illustId")
        val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
        val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
        val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)
        val density = LocalDensity.current
        val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
        val horizontalPadding = if (gridSpacingEnabled) 8.dp else 0.dp

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalItemSpacing = spacing,
            modifier = Modifier.fillMaxSize(),
            // 在 shared element transition 动画进行时禁用滚动
            userScrollEnabled = !isTransitionActive
        ) {
            // 第一张图片 - 全宽显示
            item(key = "preview_image", span = StaggeredGridItemSpan.FullLine) {
                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "illust-$illustId"),
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = IllustBoundsTransform
                            )
                    ) {
                        ProgressiveImage(
                            previewUrl = previewUrl,
                            originalUrl = firstOriginalUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            onClick = {
                                onImageClick?.invoke(previewUrl, firstOriginalUrl)
                            }
                        )
                    }
                }
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
                        Text(text = error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 详情内容
            illust?.let { loadedIllust ->
                // 多P作品的额外图片
                if (loadedIllust.page_count > 1) {
                    val additionalPages = loadedIllust.meta_pages?.drop(1) ?: emptyList()
                    additionalPages.forEachIndexed { index, page ->
                        item(key = "image_$index", span = StaggeredGridItemSpan.FullLine) {
                            val largeUrl = page.image_urls?.large ?: ""
                            val originalUrl = page.image_urls?.original
                            ProgressiveImage(
                                previewUrl = largeUrl,
                                originalUrl = originalUrl,
                                contentDescription = loadedIllust.title,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onImageClick?.invoke(largeUrl, originalUrl)
                                }
                            )
                        }
                    }
                }

                // 作者信息
                item(key = "author", span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onUserClick != null && loadedIllust.user != null) {
                                loadedIllust.user?.let { user ->
                                    onUserClick?.invoke(user.id)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(loadedIllust.user?.profile_image_urls?.medium)
                                .crossfade(true)
                                .addHeader("Referer", "https://app-api.pixiv.net/")
                                .build(),
                            contentDescription = loadedIllust.user?.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = loadedIllust.user?.name ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "@${loadedIllust.user?.account ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 统计数据
                item(key = "stats", span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = !isBookmarking) {
                                    coroutineScope.launch {
                                        isBookmarking = true
                                        try {
                                            if (isBookmarked) {
                                                PixivClient.pixivApi.deleteBookmark(illustId)
                                            } else {
                                                PixivClient.pixivApi.addBookmark(illustId)
                                            }
                                            isBookmarked = !isBookmarked
                                            // 更新 ObjectStore 中的缓存
                                            illust?.let { currentIllust ->
                                                val updatedIllust = currentIllust.copy(is_bookmarked = isBookmarked)
                                                ObjectStore.put(updatedIllust)
                                                illust = updatedIllust
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isBookmarking = false
                                        }
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isBookmarked) "取消收藏" else "收藏",
                                tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${loadedIllust.total_bookmarks ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "浏览 ${loadedIllust.total_view ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 标题和描述
                item(key = "title_caption", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = loadedIllust.title ?: "",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!loadedIllust.caption.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loadedIllust.caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 标签
                if (!loadedIllust.tags.isNullOrEmpty()) {
                    item(key = "tags", span = StaggeredGridItemSpan.FullLine) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "标签",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                loadedIllust.tags.take(5).forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tag.name ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 相关作品标题
            if (relatedState.illusts.isNotEmpty() || relatedState.isLoading) {
                item(key = "related_header", span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "相关作品",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
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
                        onRelatedIllustClick?.invoke(relatedIllust)
                    },
                    isSelectionMode = selectionManager.isSelectionMode,
                    isSelected = selectionManager.isSelected(relatedIllust.id),
                    onLongPress = { selectionManager.onLongPress(relatedIllust) },
                    onSelectionToggle = { selectionManager.toggleSelection(relatedIllust) },
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

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = relatedState.illusts
        )
    }
}
