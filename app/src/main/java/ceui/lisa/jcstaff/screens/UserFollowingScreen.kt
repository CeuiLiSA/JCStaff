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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import ceui.lisa.jcstaff.components.user.UserPreviewCard
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class UserFollowingViewModel : ViewModel() {
    private var loader: PagedDataLoader<UserPreview, UserPreviewResponse>? = null
    private var isBound = false

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PagedState<UserPreview>())
    val state: StateFlow<PagedState<UserPreview>> = _state

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
        viewModelScope.launch {
            newLoader.load()
        }
        // 将 loader 的状态转发到稳定的 _state
        viewModelScope.launch {
            newLoader.state.collect { _state.value = it }
        }
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
