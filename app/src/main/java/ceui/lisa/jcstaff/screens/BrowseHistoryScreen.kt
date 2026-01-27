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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.history.BrowseHistoryViewModel
import ceui.lisa.jcstaff.network.Illust

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BrowseHistoryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: BrowseHistoryViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()
    val selectionManager = rememberSelectionManager()
    var showClearDialog by remember { mutableStateOf(false) }

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
                        if (state.illusts.isNotEmpty()) {
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
            if (state.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    isLoading = state.isLoading,
                    error = state.error,
                    selectionManager = selectionManager,
                    gridStateKey = "browse_history"
                )
            }
        }

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
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
                        viewModel.clearAll()
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
