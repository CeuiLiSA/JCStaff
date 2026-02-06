package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import ceui.lisa.jcstaff.components.user.UserPreviewCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

class UserFollowingViewModel(
    userId: Long
) : PagedViewModel<UserPreview, UserPreviewResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/user/following",
        queryParams = mapOf("user_id" to userId.toString())
    ),
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
) {
    companion object {
        fun factory(userId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFollowingViewModel(userId) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFollowingScreen(
    userId: Long
) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: UserFollowingViewModel = viewModel(
        key = "user_following_$userId",
        factory = UserFollowingViewModel.factory(userId)
    )

    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Detect scroll to bottom
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (state.canLoadMore) {
                    viewModel.loadMore()
                }
            }
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
                            UserPreviewCard(
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
