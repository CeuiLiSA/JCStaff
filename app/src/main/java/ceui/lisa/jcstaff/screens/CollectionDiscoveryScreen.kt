package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.home.CollectionDiscoveryViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.CollectionSummary
import ceui.lisa.jcstaff.network.WebTagTranslation
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDiscoveryScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: CollectionDiscoveryViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("珍藏册") },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.isEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.isEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .clickable { viewModel.refresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击重试",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // ── 发现珍藏册: 标签 chips ──
                    if (state.recommendedTags.isNotEmpty()) {
                        item(key = "discover_title") {
                            SectionHeader(title = "发现珍藏册")
                        }
                        item(key = "discover_chips") {
                            TagChipsRow(
                                tags = state.recommendedTags,
                                tagTranslation = state.tagTranslation
                            )
                        }
                    }

                    // ── 推荐珍藏册 ──
                    if (state.recommendCollections.isNotEmpty()) {
                        item(key = "recommend_title") {
                            SectionHeader(title = "推荐珍藏册")
                        }
                        item(key = "recommend_row") {
                            CollectionRow(
                                collections = state.recommendCollections,
                                coverUrls = state.coverUrls,
                                onCollectionClick = { collection ->
                                    collection.id?.let { id ->
                                        navViewModel.navigate(NavRoute.CollectionDetail(collectionId = id))
                                    }
                                }
                            )
                        }
                    }

                    // ── 大家的珍藏册 ──
                    if (state.everyoneCollections.isNotEmpty()) {
                        item(key = "everyone_title") {
                            SectionHeader(
                                title = "大家的珍藏册",
                                subtitle = "查看全部"
                            )
                        }
                        item(key = "everyone_row") {
                            CollectionRow(
                                collections = state.everyoneCollections,
                                coverUrls = state.coverUrls,
                                onCollectionClick = { collection ->
                                    collection.id?.let { id ->
                                        navViewModel.navigate(NavRoute.CollectionDetail(collectionId = id))
                                    }
                                }
                            )
                        }
                    }

                    // ── 标签分类 ──
                    state.tagGroups.forEach { group ->
                        item(key = "tag_header_${group.tag}") {
                            SectionHeader(
                                title = "#${group.tag} 相关推荐珍藏册",
                                subtitle = "查看全部"
                            )
                        }
                        item(key = "tag_row_${group.tag}") {
                            CollectionRow(
                                collections = group.collections,
                                coverUrls = state.coverUrls,
                                onCollectionClick = { collection ->
                                    collection.id?.let { id ->
                                        navViewModel.navigate(NavRoute.CollectionDetail(collectionId = id))
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChipsRow(
    tags: List<String>,
    tagTranslation: Map<String, WebTagTranslation>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val translation = tagTranslation[tag]
            val displayEn = translation?.en?.takeIf { it.isNotEmpty() }

            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (displayEn != null) {
                    Text(
                        text = displayEn,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CollectionRow(
    collections: List<CollectionSummary>,
    coverUrls: Map<String, String> = emptyMap(),
    onCollectionClick: (CollectionSummary) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = collections,
            key = { it.id ?: it.hashCode() }
        ) { collection ->
            CollectionCard(
                collection = collection,
                coverUrl = collection.id?.let { coverUrls[it] },
                onClick = { onCollectionClick(collection) }
            )
        }
    }
}

/**
 * 珍藏册卡片
 * coverUrl 由 ViewModel 提前从搜索响应中提取（i.pximg.net），无需额外 API 请求。
 * 无封面时显示用户头像 + 收藏数。
 */
@Composable
private fun CollectionCard(
    collection: CollectionSummary,
    coverUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail: coverUrl (from illust thumbnails) > thumbnailImageUrl (skip embed.pixiv.net)
        val coverImageUrl = coverUrl
            ?: collection.thumbnailImageUrl?.takeIf {
                it.isNotBlank() && !it.contains("embed.pixiv.net")
            }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // 底层：用户头像 fallback（始终渲染，被封面覆盖时不可见）
            if (collection.profileImageUrl != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(collection.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    if (collection.bookmarkCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${collection.bookmarkCount} 收藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 上层：封面图（加载成功时覆盖头像）
            if (coverImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = collection.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title + bookmark icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = collection.title ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Author
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (collection.profileImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(collection.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = collection.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = collection.userName ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
