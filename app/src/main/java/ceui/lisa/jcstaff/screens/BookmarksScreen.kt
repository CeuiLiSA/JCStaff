package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ceui.lisa.jcstaff.R
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BookmarksScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    userId: Long,
    onBackClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    viewModel: IllustListViewModel = viewModel(key = "bookmarks_$userId")
) {
    val state by viewModel.state.collectAsState()
    val selectionManager = rememberSelectionManager()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 绑定加载器
    LaunchedEffect(userId) {
        viewModel.bind(IllustLoader {
            PixivClient.pixivApi.getUserBookmarks(userId)
        })
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.my_bookmarks)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            IllustGrid(
                illusts = state.illusts,
                onIllustClick = onIllustClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                error = state.error,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                selectionManager = selectionManager,
                gridStateKey = "bookmarks_$userId"
            )
        }

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}
