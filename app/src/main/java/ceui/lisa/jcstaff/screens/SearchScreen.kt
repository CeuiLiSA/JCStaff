package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.search.SearchViewModel

/**
 * MD3 搜索页面
 * 使用 Material3 SearchBar 组件实现标准搜索动画
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onBackClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    onUserClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    // 进入页面时自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 返回键处理
    BackHandler {
        if (expanded && query.isNotEmpty()) {
            query = ""
        } else {
            onBackClick()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {
                        expanded = false
                        if (query.isNotBlank()) {
                            viewModel.search(query)
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("搜索插画、用户...") },
                    leadingIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除"
                                )
                            }
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = if (expanded) 0.dp else 16.dp)
        ) {
            // 搜索建议和历史记录
            SearchSuggestions(
                recentSearches = state.recentSearches,
                onSuggestionClick = { suggestion ->
                    query = suggestion
                    expanded = false
                    viewModel.search(suggestion)
                },
                onClearHistory = { viewModel.clearSearchHistory() }
            )
        }

        // 搜索结果
        if (!expanded) {
            SearchResults(
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                state = state,
                onIllustClick = onIllustClick,
                onUserClick = onUserClick,
                onLoadMore = { viewModel.loadMore() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp)
            )
        }
    }
}

@Composable
private fun SearchSuggestions(
    recentSearches: List<String>,
    onSuggestionClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近搜索",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "清除",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClearHistory() }
                    )
                }
            }

            items(recentSearches) { search ->
                ListItem(
                    headlineContent = { Text(search) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { onSuggestionClick(search) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "搜索插画、用户或标签",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResults(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    state: ceui.lisa.jcstaff.search.SearchState,
    onIllustClick: (Illust) -> Unit,
    onUserClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val horizontalPadding = if (gridSpacingEnabled) 8.dp else 0.dp

    Box(modifier = modifier) {
        when {
            state.isLoading && state.illusts.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null && state.illusts.isEmpty() -> {
                Text(
                    text = state.error ?: "搜索失败",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            state.illusts.isEmpty() && !state.isLoading && state.hasSearched -> {
                Text(
                    text = "没有找到相关结果",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            state.illusts.isNotEmpty() -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
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
                            showIllustInfo = showIllustInfo,
                            cornerRadius = illustCornerRadius
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

                // 加载更多逻辑
                LaunchedEffect(state.illusts.size, state.canLoadMore) {
                    if (state.canLoadMore && !state.isLoadingMore) {
                        onLoadMore()
                    }
                }
            }
        }
    }
}
