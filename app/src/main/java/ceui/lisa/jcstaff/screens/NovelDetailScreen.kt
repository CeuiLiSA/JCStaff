package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.components.comment.CommentPreviewSection
import ceui.lisa.jcstaff.components.illust.IllustAuthorRow
import ceui.lisa.jcstaff.components.illust.IllustCaption
import ceui.lisa.jcstaff.components.illust.IllustTags
import ceui.lisa.jcstaff.components.novel.NovelActionBar
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.StoreKey
import ceui.lisa.jcstaff.core.StoreType
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.utils.formatRelativeDate
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelId: Long
) {
    val navViewModel = LocalNavigationViewModel.current
    // Try to get from ObjectStore first
    val novelFlow = ObjectStore.get<Novel>(StoreKey(novelId, StoreType.NOVEL))
    var novel by remember { mutableStateOf(novelFlow?.value) }
    // 关注状态：直接观察 ObjectStore 中的 User，确保跨页面同步
    val userId = novel?.user?.id
    val observedUser by remember(userId) {
        userId?.let { ObjectStore.get<User>(StoreKey(it, StoreType.USER)) }
    }?.collectAsState() ?: remember { mutableStateOf(null) }
    val isFollowed = observedUser?.is_followed ?: novel?.user?.is_followed ?: false
    var isBookmarked by remember { mutableStateOf(novel?.is_bookmarked ?: false) }

    // Observe ObjectStore changes
    val observedNovel by novelFlow?.collectAsState() ?: run {
        LaunchedEffect(novelId) {
            try {
                val response = PixivClient.pixivApi.getNovelDetail(novelId)
                response.novel?.let { ObjectStore.put(it) }
            } catch (_: Exception) { }
        }
        val fallbackFlow = ObjectStore.get<Novel>(StoreKey(novelId, StoreType.NOVEL))
        fallbackFlow?.collectAsState() ?: return
    }

    // Sync observed novel to local state
    LaunchedEffect(observedNovel) {
        novel = observedNovel
        isBookmarked = observedNovel.is_bookmarked ?: false
    }

    // 记录小说浏览历史
    LaunchedEffect(novel) {
        novel?.let { BrowseHistoryRepository.recordNovel(it) }
    }

    val loadedNovel = novel ?: return
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = loadedNovel.title ?: stringResource(R.string.novel_detail),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Cover image
            val coverUrl = loadedNovel.image_urls?.medium
                ?: loadedNovel.image_urls?.large
                ?: loadedNovel.image_urls?.square_medium

            if (coverUrl != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = loadedNovel.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Title
            Text(
                text = loadedNovel.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // Author row (reuse IllustAuthorRow)
            IllustAuthorRow(
                user = loadedNovel.user,
                isFollowed = isFollowed,
                onFollowStateChanged = { },
                onUserClick = { userId ->
                    navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                }
            )

            // Action bar (bookmark, text length, views)
            NovelActionBar(
                novel = loadedNovel,
                isBookmarked = isBookmarked,
                onBookmarkStateChanged = { newState, updatedNovel ->
                    isBookmarked = newState
                    novel = updatedNovel
                }
            )

            // Badges row (series + original)
            val hasSeries = loadedNovel.series != null && loadedNovel.series.title != null
            val isOriginal = loadedNovel.is_original == true

            if (hasSeries || isOriginal) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasSeries) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = loadedNovel.series!!.title!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (isOriginal) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.novel_original),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Publish date
            loadedNovel.create_date?.let { dateStr ->
                formatRelativeDate(dateStr)?.let { formatted ->
                    Text(
                        text = "${stringResource(R.string.publish_date)}: $formatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Tags (reuse IllustTags)
            if (!loadedNovel.tags.isNullOrEmpty()) {
                IllustTags(
                    tags = loadedNovel.tags,
                    onTagClick = { tag ->
                        BrowseHistoryRepository.recordSearch(tag)
                        navViewModel.navigate(NavRoute.TagDetail(tag = tag))
                    }
                )
            }

            // Caption (reuse IllustCaption)
            if (!loadedNovel.caption.isNullOrBlank()) {
                IllustCaption(caption = loadedNovel.caption)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Comment preview
            CommentPreviewSection(
                objectId = novelId,
                objectType = "novel",
                onViewAll = {
                    navViewModel.navigate(NavRoute.CommentDetail(
                        objectId = novelId,
                        objectType = "novel"
                    ))
                }
            )

            // Bottom spacing
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
