package ceui.lisa.jcstaff.home

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.components.ErrorRetryState
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.IllustFeedCard
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.NovelCard
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Pickup
import ceui.lisa.jcstaff.utils.formatCount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouFeedPage() {
    val vm: HomeAllViewModel = viewModel()
    val state by vm.state.collectAsState()
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Infinite scroll trigger
    LaunchedEffect(listState, state.canLoadMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && state.canLoadMore && !state.isLoadingMore }
            .collect { vm.loadMore() }
    }

    var userPulled by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) userPulled = false
    }

    PullToRefreshBox(
        isRefreshing = userPulled && state.isLoading,
        onRefresh = {
            if (!state.isLoading) {
                userPulled = true
                vm.refresh()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            state.error != null && state.items.isEmpty() -> {
                ErrorRetryState(
                    error = state.error ?: stringResource(R.string.load_error),
                    onRetry = { vm.refresh() }
                )
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_content),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = state.items.size,
                        key = { index ->
                            when (val item = state.items[index]) {
                                is HomeAllUiItem.IllustItem -> "illust_${item.illust.id}"
                                is HomeAllUiItem.NovelItem -> "novel_${item.novel.id}"
                                is HomeAllUiItem.TagsCarouselItem -> "carousel_${item.index}"
                                is HomeAllUiItem.SeparatorItem -> "separator_${item.index}"
                            }
                        }
                    ) { index ->
                        when (val item = state.items[index]) {
                            is HomeAllUiItem.IllustItem -> {
                                Column {
                                    IllustFeedCard(
                                        illust = item.illust,
                                        onClick = {
                                            navViewModel.navigate(
                                                NavRoute.IllustDetail(
                                                    illustId = item.illust.id,
                                                    title = item.illust.title ?: "",
                                                    previewUrl = item.illust.previewUrl(),
                                                    aspectRatio = item.illust.aspectRatio()
                                                )
                                            )
                                        },
                                        onUserClick = {
                                            item.illust.user?.id?.let { userId ->
                                                navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                                            }
                                        },
                                        onTagClick = { tag ->
                                            BrowseHistoryRepository.recordSearch(tag)
                                            navViewModel.navigate(NavRoute.TagDetail(tag = tag))
                                        },
                                        onBookmarkClick = { vm.toggleBookmark(item.illust) },
                                        onShareClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "https://www.pixiv.net/artworks/${item.illust.id}"
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, null)
                                            )
                                        }
                                    )
                                    if (item.pickup != null) {
                                        PickupCommentRow(
                                            pickup = item.pickup,
                                            onUserClick = {
                                                if (item.pickup.user_id > 0) {
                                                    navViewModel.navigate(
                                                        NavRoute.UserProfile(userId = item.pickup.user_id)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            is HomeAllUiItem.NovelItem -> {
                                NovelCard(
                                    novel = item.novel,
                                    onClick = {
                                        navViewModel.navigate(NavRoute.NovelDetail(novelId = item.novel.id))
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            is HomeAllUiItem.TagsCarouselItem -> {
                                TagsCarouselRow(
                                    item = item,
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
                            }
                            is HomeAllUiItem.SeparatorItem -> {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Loading more indicator
                    if (state.isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
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
        }
    }
}

/**
 * Instagram-style "[avatar] user_name: comment" row
 */
@Composable
private fun PickupCommentRow(
    pickup: Pickup,
    onUserClick: () -> Unit
) {
    if (pickup.comment.isNullOrBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircleAvatar(
                imageUrl = pickup.profile_image_url,
                size = 24.dp,
                contentDescription = pickup.user_name,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onUserClick)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = pickup.user_name ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onUserClick)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = pickup.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (pickup.comment_count > 1) {
            Text(
                text = stringResource(R.string.pickup_more_comments, pickup.comment_count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Horizontal scrolling carousel for trending tags / related illusts
 */
@Composable
private fun TagsCarouselRow(
    item: HomeAllUiItem.TagsCarouselItem,
    onIllustClick: (Illust) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (!item.tag.isNullOrBlank()) {
            Text(
                text = "#${item.tag}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = item.illusts,
                key = { it.id }
            ) { illust ->
                IllustCard(
                    illust = illust,
                    onClick = { onIllustClick(illust) },
                    showIllustInfo = true,
                    modifier = Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
