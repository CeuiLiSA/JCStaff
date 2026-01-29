package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.tagdetail.SearchSort
import ceui.lisa.jcstaff.tagdetail.SearchTarget
import ceui.lisa.jcstaff.tagdetail.TagDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tag: Tag,
    isPremium: Boolean,
    initialTab: Int = 0,
    viewModel: TagDetailViewModel = viewModel(key = "tag_detail_${tag.name}")
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()
    val selectionManager = LocalSelectionManager.current
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val novelListState = rememberLazyListState()

    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagPendingRemoval by remember { mutableStateOf<Tag?>(null) }

    val premiumHint = stringResource(R.string.premium_sort_hint)

    // Initialize with the tag
    LaunchedEffect(tag) {
        viewModel.init(tag, isPremium)
    }

    // Back handler for selection mode
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    val tabTitles = listOf(
        stringResource(R.string.tab_tag_illusts),
        stringResource(R.string.tab_tag_novels)
    )

    Box {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.tags.forEach { currentTag ->
                                val canRemove = state.tags.size > 1
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        if (canRemove) tagPendingRemoval = currentTag
                                    },
                                    label = {
                                        Text("#${currentTag.name ?: ""}")
                                    },
                                    trailingIcon = if (canRemove) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(InputChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                            AssistChip(
                                onClick = { showAddTagDialog = true },
                                label = { Text(stringResource(R.string.add_tag)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage
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
                        0 -> {
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
                                modifier = Modifier.fillMaxSize(),
                                isLoading = state.isLoading,
                                isLoadingMore = state.isLoadingMore,
                                canLoadMore = state.canLoadMore,
                                error = state.error,
                                onRefresh = { viewModel.refresh() },
                                onLoadMore = { viewModel.loadMore() },
                                                                headerContent = {
                                    item(
                                        key = "search_filter_bar",
                                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine
                                    ) {
                                        SearchFilterBar(
                                            sort = state.sort,
                                            searchTarget = state.searchTarget,
                                            isPremium = isPremium,
                                            onSortChanged = { viewModel.setSort(it) },
                                            onSearchTargetChanged = { viewModel.setSearchTarget(it) },
                                            onPremiumRequired = {
                                                coroutineScope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar(premiumHint)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SearchFilterBar(
                                    sort = state.novelSort,
                                    searchTarget = state.novelSearchTarget,
                                    isPremium = isPremium,
                                    onSortChanged = { viewModel.setNovelSort(it) },
                                    onSearchTargetChanged = { viewModel.setNovelSearchTarget(it) },
                                    onPremiumRequired = {
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(premiumHint)
                                        }
                                    }
                                )
                                NovelList(
                                    novels = state.novels,
                                    onNovelClick = { novel ->
                                        navViewModel.navigate(NavRoute.NovelDetail(novelId = novel.id))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    isLoading = state.isNovelLoading,
                                    isLoadingMore = state.isNovelLoadingMore,
                                    canLoadMore = state.canLoadMoreNovels,
                                    error = state.novelError,
                                    onRefresh = { viewModel.refreshNovels() },
                                    onLoadMore = { viewModel.loadMoreNovels() },
                                    listState = novelListState
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selection top bar overlay
        SelectionTopBar(
                        allIllusts = state.illusts
        )
    }

    // Remove tag confirmation dialog
    tagPendingRemoval?.let { pendingTag ->
        AlertDialog(
            onDismissRequest = { tagPendingRemoval = null },
            title = { Text(stringResource(R.string.remove_tag_title)) },
            text = {
                Text(stringResource(R.string.remove_tag_message, pendingTag.name ?: ""))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTag(pendingTag)
                    tagPendingRemoval = null
                }) {
                    Text(stringResource(R.string.remove_tag_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { tagPendingRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add tag dialog
    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { tagName ->
                viewModel.addTag(Tag(name = tagName))
                showAddTagDialog = false
            }
        )
    }
}

@Composable
private fun SearchFilterBar(
    sort: SearchSort,
    searchTarget: SearchTarget,
    isPremium: Boolean,
    onSortChanged: (SearchSort) -> Unit,
    onSearchTargetChanged: (SearchTarget) -> Unit,
    onPremiumRequired: () -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            FilterChip(
                selected = sort != SearchSort.DATE_DESC,
                onClick = { sortExpanded = true },
                label = { Text(stringResource(sort.labelRes)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            )
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                SearchSort.entries.forEach { option ->
                    val enabled = when {
                        option.premiumOnly -> isPremium
                        option.nonPremiumOnly -> !isPremium
                        else -> true
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(option.labelRes),
                                color = if (enabled)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        onClick = {
                            if (enabled) {
                                onSortChanged(option)
                                sortExpanded = false
                            } else {
                                sortExpanded = false
                                if (option.premiumOnly) {
                                    onPremiumRequired()
                                }
                            }
                        }
                    )
                }
            }
        }
        Box {
            FilterChip(
                selected = searchTarget != SearchTarget.PARTIAL_MATCH_FOR_TAGS,
                onClick = { targetExpanded = true },
                label = { Text(stringResource(searchTarget.labelRes)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            )
            DropdownMenu(
                expanded = targetExpanded,
                onDismissRequest = { targetExpanded = false }
            ) {
                SearchTarget.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            onSearchTargetChanged(option)
                            targetExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_tag)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.enter_tag_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
