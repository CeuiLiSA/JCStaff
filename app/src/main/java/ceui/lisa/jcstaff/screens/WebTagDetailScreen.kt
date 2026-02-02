package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.ErrorRetryState
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.home.WebTagDetailUiState
import ceui.lisa.jcstaff.home.WebTagDetailViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.WebTagTranslation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

// 头部高度常量
private val HeaderExpandedHeight = 280.dp
private val ToolbarHeight = 60.dp  // 标准 Toolbar 高度

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WebTagDetailScreen(tag: Tag) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: WebTagDetailViewModel = viewModel(
        key = "web_tag_${tag.name}",
        factory = WebTagDetailViewModel.factory(tag)
    )
    val state by viewModel.state.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 获取状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 折叠后的高度 = 状态栏 + Toolbar
    val headerCollapsedHeight = statusBarHeight + ToolbarHeight

    val tabs = listOf(
        stringResource(R.string.pixpedia),
        stringResource(R.string.illustrations),
        stringResource(R.string.tab_novel)
    )

    // 折叠状态
    val headerExpandedHeightPx = with(density) { HeaderExpandedHeight.toPx() }
    val headerCollapsedHeightPx = with(density) { headerCollapsedHeight.toPx() }
    val headerHeightRange = headerExpandedHeightPx - headerCollapsedHeightPx

    var headerOffsetY by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetY + delta
                val consumed = when {
                    // 向上滚动，折叠 header
                    delta < 0 -> {
                        val previousOffset = headerOffsetY
                        headerOffsetY = newOffset.coerceIn(-headerHeightRange, 0f)
                        headerOffsetY - previousOffset
                    }
                    // 向下滚动，展开 header
                    delta > 0 -> {
                        val previousOffset = headerOffsetY
                        headerOffsetY = newOffset.coerceIn(-headerHeightRange, 0f)
                        headerOffsetY - previousOffset
                    }

                    else -> 0f
                }
                return Offset(0f, consumed)
            }
        }
    }

    // 折叠进度 0f = 展开, 1f = 折叠
    val collapseProgress: Float by remember {
        derivedStateOf { -headerOffsetY / headerHeightRange }
    }

    // 动态头部高度
    val headerHeight: Dp by remember {
        derivedStateOf {
            with(density) { (headerExpandedHeightPx + headerOffsetY).toDp() }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // ==================== Collapsible Hero Header ====================
            CollapsibleHeroHeader(
                state = state,
                tag = tag,
                headerHeight = headerHeight,
                minHeight = headerCollapsedHeight,
                collapseProgress = collapseProgress,
                onBackClick = { navViewModel.goBack() }
            )

            // ==================== Tab Row (Shoulder) ====================
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            width = 32.dp,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ==================== ViewPager ====================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when {
                    state.isLoading && state.illusts.isEmpty() -> {
                        LoadingIndicator()
                    }

                    state.error != null && state.illusts.isEmpty() -> {
                        ErrorRetryState(
                            error = state.error ?: "Unknown error",
                            onRetry = { viewModel.refresh() }
                        )
                    }

                    else -> {
                        when (page) {
                            0 -> PixpediaPage(
                                state = state,
                                onTagClick = { tagName ->
                                    navViewModel.navigate(NavRoute.WebTagDetail(Tag(name = tagName)))
                                }
                            )

                            1 -> IllustMangaPage(
                                state = state,
                                onIllustClick = { illust ->
                                    navViewModel.navigate(
                                        NavRoute.IllustDetail(
                                            illustId = illust.id,
                                            title = illust.title ?: "",
                                            previewUrl = illust.previewUrl(),
                                            aspectRatio = illust.aspectRatio()
                                        )
                                    )
                                }
                            )

                            2 -> NovelPage(
                                state = state,
                                onNovelClick = { novel ->
                                    navViewModel.navigate(NavRoute.NovelDetail(novelId = novel.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Collapsible Hero Header ====================

@Composable
private fun CollapsibleHeroHeader(
    state: WebTagDetailUiState,
    tag: Tag,
    headerHeight: Dp,
    minHeight: Dp,
    collapseProgress: Float,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    // 优先使用百科图片，否则使用热门作品的第一张
    val backgroundUrl = state.pixpedia?.image
        ?: state.popular?.permanent?.firstOrNull()?.url
        ?: state.popular?.recent?.firstOrNull()?.url
        ?: state.illusts.firstOrNull()?.previewUrl()

    // 内容透明度：完全折叠时隐藏详细信息
    val contentAlpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
    // 标题缩放
    val titleScale = 1f - (collapseProgress * 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight.coerceAtLeast(minHeight))
    ) {
        // 背景图
        if (backgroundUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backgroundUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 渐变占位背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
            )
        }

        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // 顶部栏（始终可见）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }

            // 折叠时显示标题
            if (collapseProgress > 0.5f) {
                Text(
                    text = tag.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = (collapseProgress - 0.5f) * 2f
                        }
                )
            }
        }

        // 底部标签信息（展开时显示）
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = titleScale
                    scaleY = titleScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                }
        ) {
            // 标签名
            Text(
                text = tag.name ?: "",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 翻译名
            val translatedName = state.translatedName?.zh
                ?: state.translatedName?.en
                ?: tag.translated_name
            if (!translatedName.isNullOrEmpty() && translatedName != tag.name) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translatedName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 统计信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatBadge(
                    icon = Icons.Default.Image,
                    count = state.illustTotal + state.mangaTotal,
                    label = stringResource(R.string.illustrations)
                )
                StatBadge(
                    icon = Icons.Default.AutoStories,
                    count = state.novelTotal,
                    label = stringResource(R.string.tab_novel)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ==================== Page 0: Pixpedia ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PixpediaPage(
    state: WebTagDetailUiState,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 百科简介
        if (!state.pixpedia?.abstract.isNullOrEmpty()) {
            SectionTitle(
                icon = Icons.Default.Info,
                title = stringResource(R.string.pixpedia)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.pixpedia?.abstract ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 多语言翻译
        state.translatedName?.let { translation ->
            TranslationSection(translation = translation)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 父标签
        state.parentTag?.let { parent ->
            SectionTitle(title = stringResource(R.string.parent_tag))
            Spacer(modifier = Modifier.height(8.dp))
            TagChip(tag = parent, onClick = { onTagClick(parent) })
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 同级标签
        if (state.siblingsTags.isNotEmpty()) {
            SectionTitle(title = stringResource(R.string.siblings_tags))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.siblingsTags.forEach { tag ->
                    TagChip(tag = tag, onClick = { onTagClick(tag) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 子标签
        if (state.childrenTags.isNotEmpty()) {
            SectionTitle(title = stringResource(R.string.children_tags))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.childrenTags.forEach { tag ->
                    TagChip(tag = tag, onClick = { onTagClick(tag) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 相关标签
        if (state.relatedTags.isNotEmpty()) {
            SectionTitle(title = stringResource(R.string.related_tags))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.relatedTags.take(20).forEach { tag ->
                    TagChip(tag = tag, onClick = { onTagClick(tag) })
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TranslationSection(translation: WebTagTranslation) {
    val translations = listOfNotNull(
        translation.zh?.let { "中文" to it },
        translation.zh_tw?.let { "繁中" to it },
        translation.en?.let { "English" to it },
        translation.ko?.let { "한국어" to it },
        translation.romaji?.let { "Romaji" to it }
    )

    if (translations.isEmpty()) return

    SectionTitle(title = stringResource(R.string.translations))
    Spacer(modifier = Modifier.height(8.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        translations.forEach { (lang, text) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ==================== Page 1: Illust & Manga ====================

@Composable
private fun IllustMangaPage(
    state: WebTagDetailUiState,
    onIllustClick: (Illust) -> Unit
) {
    val context = LocalContext.current
    val popularIllusts = (state.popular?.permanent ?: emptyList()) +
            (state.popular?.recent ?: emptyList())

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 6.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        // 热门作品横滑区域
        if (popularIllusts.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    SectionTitle(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.popular_works),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularIllusts.take(10), key = { "popular_${it.id}" }) { webIllust ->
                            val illust = webIllust.toIllust()
                            PopularIllustCard(
                                illust = illust,
                                onClick = { onIllustClick(illust) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionTitle(
                        icon = Icons.Default.Image,
                        title = stringResource(R.string.latest_illusts),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 最新插画瀑布流
        items(state.illusts, key = { it.id }) { illust ->
            IllustCard(
                illust = illust,
                onClick = { onIllustClick(illust) }
            )
        }

        // 底部间距
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PopularIllustCard(
    illust: Illust,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(illust.previewUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
            )

            // 底部渐变 + 标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = illust.title ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 多图标识
            if ((illust.page_count ?: 1) > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${illust.page_count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ==================== Page 2: Novels ====================

@Composable
private fun NovelPage(
    state: WebTagDetailUiState,
    onNovelClick: (Novel) -> Unit
) {
    if (state.novels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.novels, key = { it.id }) { novel ->
            NovelCard(
                novel = novel,
                onClick = { onNovelClick(novel) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NovelCard(
    novel: Novel,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 封面
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(novel.image_urls?.medium)
                    .crossfade(true)
                    .build(),
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp, 100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题
                Text(
                    text = novel.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 作者
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(novel.user?.profile_image_urls?.medium)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = novel.user?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 统计信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${novel.text_length ?: 0} 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    novel.total_bookmarks?.let { bookmarks ->
                        Text(
                            text = "♥ ${formatCount(bookmarks)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 简介
                novel.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = caption.replace(Regex("<[^>]*>"), "").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== Common Components ====================

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TagChip(
    tag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Tag,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = null,
        shape = RoundedCornerShape(8.dp)
    )
}

// ==================== Utils ====================

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 10_000 -> String.format("%.1fW", count / 10_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
