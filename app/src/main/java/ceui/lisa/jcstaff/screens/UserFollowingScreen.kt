package ceui.lisa.jcstaff.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class UserFollowingViewModel : ViewModel() {
    private var loader: PagedDataLoader<UserPreview, UserPreviewResponse>? = null
    private var isBound = false

    val state: StateFlow<PagedState<UserPreview>>
        get() = loader?.state ?: kotlinx.coroutines.flow.MutableStateFlow(PagedState())

    private var _state: StateFlow<PagedState<UserPreview>>? = null

    fun bind(userId: Long) {
        if (isBound) return
        isBound = true

        val newLoader = PagedDataLoader<UserPreview, UserPreviewResponse>(
            cacheConfig = null,
            responseClass = UserPreviewResponse::class.java,
            loadFirstPage = { PixivClient.pixivApi.getUserFollowing(userId) },
            onItemsLoaded = { previews ->
                previews.forEach { preview ->
                    preview.user?.let { ObjectStore.put(it) }
                    preview.illusts.forEach { illust ->
                        ObjectStore.put(illust)
                        illust.user?.let { ObjectStore.put(it) }
                    }
                }
            }
        )
        loader = newLoader
        _state = newLoader.state
        viewModelScope.launch { newLoader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader?.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader?.refresh() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFollowingScreen(
    userId: Long
) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: UserFollowingViewModel = viewModel(key = "user_following_$userId")

    LaunchedEffect(userId) {
        viewModel.bind(userId)
    }

    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Detect scroll to bottom
    LaunchedEffect(listState, state.canLoadMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && state.canLoadMore }
            .collect { viewModel.loadMore() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_following)) },
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
    ) { paddingValues ->
        var userPulled by remember { mutableStateOf(false) }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) userPulled = false
        }

        PullToRefreshBox(
            isRefreshing = userPulled && state.isLoading,
            onRefresh = {
                if (!state.isLoading) {
                    userPulled = true
                    viewModel.refresh()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.items.isEmpty() && state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.items, key = { it.user?.id ?: 0L }) { userPreview ->
                            FollowingUserCard(
                                userPreview = userPreview,
                                onUserClick = {
                                    userPreview.user?.id?.let { id ->
                                        navViewModel.navigate(NavRoute.UserProfile(userId = id))
                                    }
                                },
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

                        if (state.isLoadingMore) {
                            item {
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
                }
            }
        }
    }
}

@Composable
private fun FollowingUserCard(
    userPreview: UserPreview,
    onUserClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = userPreview.user ?: return

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // User info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.profile_image_urls?.findAvatarUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!user.comment.isNullOrBlank()) {
                        Text(
                            text = user.comment ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Illusts preview
            if (userPreview.illusts.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(userPreview.illusts.take(3), key = { it.id }) { illust ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(illust.image_urls?.square_medium ?: illust.previewUrl())
                                .crossfade(true)
                                .build(),
                            contentDescription = illust.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 100.dp, height = 100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onIllustClick(illust) }
                        )
                    }
                }
            }
        }
    }
}
