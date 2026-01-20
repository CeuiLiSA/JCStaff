package ceui.lisa.jcstaff.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/**
 * 渐进式图片加载组件
 */
@Composable
private fun ProgressiveImage(
    previewUrl: String,
    originalUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    val context = LocalContext.current
    var isOriginalLoaded by remember(originalUrl) { mutableStateOf(false) }

    Box(modifier = modifier) {
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

        if (originalUrl != null && originalUrl != previewUrl) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(originalUrl)
                    .addHeader("Referer", "https://app-api.pixiv.net/")
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        isOriginalLoaded = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isOriginalLoaded) 1f else 0f)
            )
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

    // 相关作品状态
    val relatedState by relatedViewModel.state.collectAsState()

    LaunchedEffect(observedIllust) {
        observedIllust?.let { illust = it }
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
            PixivClient.pixivApi.getRelatedIllusts(illustId).illusts
        })
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
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            // 第一张图片 - 全宽显示
            item(key = "preview_image", span = StaggeredGridItemSpan.FullLine) {
                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .sharedElement(
                                state = rememberSharedContentState(key = "illust-$illustId"),
                                animatedVisibilityScope = animatedContentScope
                            )
                    ) {
                        ProgressiveImage(
                            previewUrl = previewUrl,
                            originalUrl = firstOriginalUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize()
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 作者信息
                item(key = "author", span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (loadedIllust.is_bookmarked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (loadedIllust.is_bookmarked == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    }
                )
            }
        }
    }
}
