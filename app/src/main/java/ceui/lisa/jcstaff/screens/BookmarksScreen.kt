package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
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
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    userId: Long,
    viewModel: IllustListViewModel = viewModel(key = "bookmarks_$userId")
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()
    val selectionManager = LocalSelectionManager.current

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
            IllustGrid(
                illusts = state.illusts,
                onIllustClick = { illust ->
                    navViewModel.navigate(NavRoute.IllustDetail(
                        illustId = illust.id,
                        title = illust.title ?: "",
                        previewUrl = illust.previewUrl(),
                        aspectRatio = illust.aspectRatio()
                    ))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                error = state.error,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                            )
        }

        // Selection top bar overlay
        SelectionTopBar(allIllusts = state.illusts)
    }
}
