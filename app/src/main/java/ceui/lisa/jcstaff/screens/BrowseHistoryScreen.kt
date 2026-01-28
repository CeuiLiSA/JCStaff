package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.NovelCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.components.UserHistoryCard
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.history.BrowseHistoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BrowseHistoryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: BrowseHistoryViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current
    val illustState by viewModel.illustState.collectAsState()
    val novelState by viewModel.novelState.collectAsState()
    val userState by viewModel.userState.collectAsState()
    val selectionManager = rememberSelectionManager()
    var showClearDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val tabTitles = listOf(
        stringResource(R.string.tab_illust),
        stringResource(R.string.tab_novel),
        stringResource(R.string.tab_user)
    )

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.browse_history)) },
                    navigationIcon = {
                        IconButton(onClick = { navViewModel.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        val hasContent = when (pagerState.currentPage) {
                            0 -> illustState.illusts.isNotEmpty()
                            1 -> novelState.novels.isNotEmpty()
                            2 -> userState.users.isNotEmpty()
                            else -> false
                        }
                        if (hasContent) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.clear_history)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> IllustHistoryPage(
                            illustState = illustState,
                            selectionManager = selectionManager,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
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
                        1 -> NovelHistoryPage(
                            novelState = novelState,
                            onNovelClick = { novel ->
                                navViewModel.navigate(NavRoute.NovelDetail(novelId = novel.id))
                            }
                        )
                        2 -> UserHistoryPage(
                            userState = userState,
                            onUserClick = { user ->
                                navViewModel.navigate(NavRoute.UserProfile(userId = user.id))
                            }
                        )
                    }
                }
            }
        }

        // Selection top bar overlay (仅插画页有效)
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = illustState.illusts
        )
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> viewModel.clearIllustHistory()
                            1 -> viewModel.clearNovelHistory()
                            2 -> viewModel.clearUserHistory()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun IllustHistoryPage(
    illustState: ceui.lisa.jcstaff.history.IllustHistoryState,
    selectionManager: ceui.lisa.jcstaff.core.SelectionManager,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onIllustClick: (ceui.lisa.jcstaff.network.Illust) -> Unit
) {
    if (illustState.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_browse_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        IllustGrid(
            illusts = illustState.illusts,
            onIllustClick = onIllustClick,
            modifier = Modifier.fillMaxSize(),
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            isLoading = illustState.isLoading,
            error = illustState.error,
            selectionManager = selectionManager,
            gridStateKey = "browse_history_illust"
        )
    }
}

@Composable
private fun NovelHistoryPage(
    novelState: ceui.lisa.jcstaff.history.NovelHistoryState,
    onNovelClick: (ceui.lisa.jcstaff.network.Novel) -> Unit
) {
    if (novelState.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_novel_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(novelState.novels, key = { it.id }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UserHistoryPage(
    userState: ceui.lisa.jcstaff.history.UserHistoryState,
    onUserClick: (ceui.lisa.jcstaff.network.User) -> Unit
) {
    if (userState.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_user_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(userState.users, key = { it.id }) { user ->
                UserHistoryCard(
                    user = user,
                    onClick = { onUserClick(user) }
                )
            }
        }
    }
}
