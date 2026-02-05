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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
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
import ceui.lisa.jcstaff.tagdetail.TagIllustSearchViewModel
import ceui.lisa.jcstaff.tagdetail.TagNovelSearchViewModel
import ceui.lisa.jcstaff.tagdetail.TagUserSearchViewModel
import ceui.lisa.jcstaff.screens.UserSearchPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tag: Tag,
    isPremium: Boolean,
    initialTab: Int = 0,
    illustViewModel: TagIllustSearchViewModel = viewModel(key = "tag_illust_${tag.name}"),
    novelViewModel: TagNovelSearchViewModel = viewModel(key = "tag_novel_${tag.name}"),
    userViewModel: TagUserSearchViewModel = viewModel(key = "tag_user_${tag.name}")
) {
    val navViewModel = LocalNavigationViewModel.current
    val illustState by illustViewModel.state.collectAsState()
    val novelState by novelViewModel.state.collectAsState()
    val userState by userViewModel.state.collectAsState()
    val selectionManager = LocalSelectionManager.current
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val novelListState = rememberLazyListState()

    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagPendingRemoval by remember { mutableStateOf<Tag?>(null) }

    val premiumHint = stringResource(R.string.premium_sort_hint)

    // Initialize all ViewModels with the tag
    LaunchedEffect(tag) {
        illustViewModel.init(tag, isPremium)
        novelViewModel.init(tag, isPremium)
        userViewModel.init(tag)
    }

    // Back handler for selection mode
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    val tabTitles = listOf(
        stringResource(R.string.tab_illust_manga),
        stringResource(R.string.tab_tag_novels),
        stringResource(R.string.tab_user)
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
                            illustState.tags.forEach { currentTag ->
                                val canRemove = illustState.tags.size > 1
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
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                width = 32.dp,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
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
                                illusts = illustState.illusts,
                                onIllustClick = { illust ->
                                    navViewModel.navigate(NavRoute.IllustDetail(
                                        illustId = illust.id,
                                        title = illust.title ?: "",
                                        previewUrl = illust.previewUrl(),
                                        aspectRatio = illust.aspectRatio()
                                    ))
                                },
                                modifier = Modifier.fillMaxSize(),
                                isLoading = illustState.isLoading,
                                isLoadingMore = illustState.isLoadingMore,
                                canLoadMore = illustState.canLoadMore,
                                error = illustState.error,
                                onRefresh = { illustViewModel.refresh() },
                                onLoadMore = { illustViewModel.loadMore() },
                                headerContent = {
                                    item(
                                        key = "search_filter_bar",
                                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine
                                    ) {
                                        SearchFilterBar(
                                            sort = illustState.sort,
                                            searchTarget = illustState.searchTarget,
                                            isPremium = isPremium,
                                            onSortChanged = { illustViewModel.setSort(it) },
                                            onSearchTargetChanged = { illustViewModel.setSearchTarget(it) },
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
                                    sort = novelState.sort,
                                    searchTarget = novelState.searchTarget,
                                    isPremium = isPremium,
                                    onSortChanged = { novelViewModel.setSort(it) },
                                    onSearchTargetChanged = { novelViewModel.setSearchTarget(it) },
                                    onPremiumRequired = {
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(premiumHint)
                                        }
                                    }
                                )
                                NovelList(
                                    novels = novelState.novels,
                                    onNovelClick = { novel ->
                                        navViewModel.navigate(NavRoute.NovelDetail(novelId = novel.id))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    isLoading = novelState.isLoading,
                                    isLoadingMore = novelState.isLoadingMore,
                                    canLoadMore = novelState.canLoadMore,
                                    error = novelState.error,
                                    onRefresh = { novelViewModel.refresh() },
                                    onLoadMore = { novelViewModel.loadMore() },
                                    listState = novelListState
                                )
                            }
                        }
                        2 -> {
                            UserSearchPage(
                                userPreviews = userState.items,
                                isLoading = userState.isLoading,
                                isLoadingMore = userState.isLoadingMore,
                                canLoadMore = userState.canLoadMore,
                                error = userState.error,
                                onRefresh = { userViewModel.refresh() },
                                onLoadMore = { userViewModel.loadMore() }
                            )
                        }
                    }
                }
            }
        }

        // Selection top bar overlay
        SelectionTopBar(
            allIllusts = illustState.illusts
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
                    illustViewModel.removeTag(pendingTag)
                    novelViewModel.removeTag(pendingTag)
                    userViewModel.removeTag(pendingTag)
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
                val newTag = Tag(name = tagName)
                illustViewModel.addTag(newTag)
                novelViewModel.addTag(newTag)
                userViewModel.addTag(newTag)
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
