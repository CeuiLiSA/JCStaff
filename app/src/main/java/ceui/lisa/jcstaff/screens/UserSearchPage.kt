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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.user.UserPreviewCard
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.UserPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchPage(
    userPreviews: List<UserPreview>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navViewModel = LocalNavigationViewModel.current
    val listState = rememberLazyListState()

    // Detect scroll to bottom
    LaunchedEffect(listState, canLoadMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && canLoadMore }
            .collect { onLoadMore() }
    }

    var userPulled by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) userPulled = false
    }

    PullToRefreshBox(
        isRefreshing = userPulled && isLoading,
        onRefresh = {
            if (!isLoading) {
                userPulled = true
                onRefresh()
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            userPreviews.isEmpty() && isLoading -> {
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
                    items(userPreviews, key = { it.user?.id ?: 0L }) { userPreview ->
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

                    if (isLoadingMore) {
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
