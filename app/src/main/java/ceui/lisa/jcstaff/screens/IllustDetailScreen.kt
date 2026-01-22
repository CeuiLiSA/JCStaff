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
import androidx.compose.foundation.layout.FlowRow
import android.widget.Toast
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProgressiveImage(
    previewUrl: String,
    originalUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
    sharedElementKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
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

    // 计算图片的 shared element modifier
    val imageModifier = if (sharedElementKey != null && sharedTransitionScope != null && animatedContentScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "image-$sharedElementKey"),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = IllustBoundsTransform
            )
        }
    } else {
        Modifier
    }

    // 计算进度指示器的 shared element modifier
    val progressModifier = if (sharedElementKey != null && sharedTransitionScope != null && animatedContentScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "progress-$sharedElementKey"),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = IllustBoundsTransform
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // 图片容器（可能带 shared element transition）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(imageModifier)
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
        }

        // 原图加载中的进度指示器（带百分比，可能带 shared element transition）
        if (originalUrl != null && originalUrl != previewUrl && isTaskLoading && !isTaskCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .then(progressModifier)
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
    onImageClick: ((previewUrl: String, originalUrl: String?, sharedElementKey: String) -> Unit)? = null,
    onUserClick: ((Long) -> Unit)? = null,
    relatedViewModel: IllustListViewModel = viewModel(key = "related_$illustId")
) {
    val context = LocalContext.current

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
        Scaffold { paddingValues ->
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
                top = 0.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalItemSpacing = spacing,
            modifier = Modifier.fillMaxSize()
        ) {
            // 第一张图片 - 全宽沉浸式显示
            item(key = "preview_image", span = StaggeredGridItemSpan.FullLine) {
                val firstImageKey = "${illustId}_0"
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
                            sharedElementKey = firstImageKey,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            onClick = {
                                onImageClick?.invoke(previewUrl, firstOriginalUrl, firstImageKey)
                            }
                        )

                        // 浮动返回按钮
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 36.dp, start = 8.dp)
                                .size(40.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
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
                            val pageIndex = index + 1 // 从1开始，因为0是第一张图
                            val imageKey = "${illustId}_$pageIndex"
                            val largeUrl = page.image_urls?.large ?: ""
                            val originalUrl = page.image_urls?.original
                            ProgressiveImage(
                                previewUrl = largeUrl,
                                originalUrl = originalUrl,
                                contentDescription = loadedIllust.title,
                                modifier = Modifier.fillMaxWidth(),
                                sharedElementKey = imageKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                onClick = {
                                    onImageClick?.invoke(largeUrl, originalUrl, imageKey)
                                }
                            )
                        }
                    }
                }

                // 标题
                item(key = "title", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = loadedIllust.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }

                // 作者信息区域
                item(key = "author", span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onUserClick != null && loadedIllust.user != null) {
                                loadedIllust.user?.let { user ->
                                    onUserClick?.invoke(user.id)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                .size(40.dp)
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${loadedIllust.user?.account ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 操作按钮行（收藏、下载、浏览数）
                item(key = "actions", span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 收藏按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
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
                                .background(
                                    if (isBookmarked) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = formatCount(loadedIllust.total_bookmarks ?: 0),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isBookmarked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        // 下载按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = !isDownloading) {
                                    val downloadUrl = firstOriginalUrl ?: previewUrl
                                    coroutineScope.launch {
                                        isDownloading = true
                                        val fileName = "pixiv_${illustId}_${System.currentTimeMillis()}"
                                        val cachedFilePath = LoadTaskManager.getCachedFilePath(downloadUrl)
                                        val result = if (cachedFilePath != null) {
                                            ImageDownloader.saveFromCacheToGallery(
                                                context = context,
                                                cachedFilePath = cachedFilePath,
                                                fileName = fileName
                                            )
                                        } else {
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
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "下载",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 浏览数
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formatCount(loadedIllust.total_view ?: 0),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // 元信息区域
                item(key = "meta_info", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 发布时间
                        loadedIllust.create_date?.let { dateStr ->
                            val formattedDate = formatDate(dateStr)
                            if (formattedDate != null) {
                                MetaInfoRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = "发布时间",
                                    value = formattedDate
                                )
                            }
                        }

                        // 图片尺寸
                        if (loadedIllust.width > 0 && loadedIllust.height > 0) {
                            MetaInfoRow(
                                icon = Icons.Default.Photo,
                                label = "尺寸",
                                value = "${loadedIllust.width} × ${loadedIllust.height}"
                            )
                        }

                        // 页数
                        if (loadedIllust.page_count > 1) {
                            MetaInfoRow(
                                icon = Icons.Default.PhotoLibrary,
                                label = "页数",
                                value = "${loadedIllust.page_count} 张"
                            )
                        }

                        // 作品类型
                        val typeText = when {
                            loadedIllust.isGif() -> "动图 (Ugoira)"
                            loadedIllust.isManga() -> "漫画"
                            else -> "插画"
                        }
                        MetaInfoRow(
                            icon = when {
                                loadedIllust.isGif() -> Icons.Default.PlayCircle
                                loadedIllust.isManga() -> Icons.Default.PhotoLibrary
                                else -> Icons.Default.Image
                            },
                            label = "类型",
                            value = typeText
                        )

                        // AI 类型
                        if (loadedIllust.illust_ai_type > 0) {
                            val aiText = when (loadedIllust.illust_ai_type) {
                                1 -> "AI 辅助创作"
                                2 -> "AI 生成"
                                else -> "AI 相关"
                            }
                            MetaInfoRow(
                                icon = Icons.Default.AutoAwesome,
                                label = "AI",
                                value = aiText,
                                valueColor = MaterialTheme.colorScheme.tertiary
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
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "标签",
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
                                loadedIllust.tags.forEach { tag ->
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .clickable { /* TODO: 搜索标签 */ }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "#${tag.name ?: ""}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        tag.translated_name?.let { translated ->
                                            Text(
                                                text = translated,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 作品简介
                if (!loadedIllust.caption.isNullOrBlank()) {
                    item(key = "caption", span = StaggeredGridItemSpan.FullLine) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "简介",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = loadedIllust.caption.replace(Regex("<[^>]*>"), ""), // 简单去除 HTML 标签
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }
            }

            // 相关作品标题
            if (relatedState.illusts.isNotEmpty() || relatedState.isLoading) {
                item(key = "related_header", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "相关作品",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
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

/**
 * 元信息行组件
 */
@Composable
private fun MetaInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * 格式化数字显示
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 100000 -> String.format(Locale.US, "%.1fw", count / 10000.0)
        count >= 10000 -> String.format(Locale.US, "%.1f万", count / 10000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        else -> count.toString()
    }
}

/**
 * 格式化日期显示
 */
private fun formatDate(dateStr: String): String? {
    return try {
        val zonedDateTime = ZonedDateTime.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        null
    }
}
