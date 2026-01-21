package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.SettingsStore
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
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
            PixivClient.pixivApi.getUserBookmarks(userId).illusts
        })
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("我的收藏") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    state.hasError && state.isEmpty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.error ?: "加载失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "下拉刷新重试",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    state.isEmpty && state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        val gridState = rememberPersistentLazyStaggeredGridState("bookmarks_$userId")
                        val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
                        val density = LocalDensity.current
                        val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
                        val contentPadding = if (gridSpacingEnabled) PaddingValues(8.dp) else PaddingValues(0.dp)

                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = gridState,
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalItemSpacing = spacing,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.illusts, key = { it.id }) { illust ->
                                IllustCard(
                                    illust = illust,
                                    onClick = { onIllustClick(illust) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    isSelectionMode = selectionManager.isSelectionMode,
                                    isSelected = selectionManager.isSelected(illust.id),
                                    onLongPress = { selectionManager.onLongPress(illust) },
                                    onSelectionToggle = { selectionManager.toggleSelection(illust) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}
