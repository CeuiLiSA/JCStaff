package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 搜索页面
 * 支持关键词联想（debounce 500ms）和搜索历史
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun SearchScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val searchHistory by BrowseHistoryRepository.getSearchHistoryFlow().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // 进入页面时自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounce 关键词联想
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(500)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .collectLatest { word ->
                isLoadingSuggestions = true
                try {
                    val response = PixivClient.pixivApi.searchAutocomplete(word.trim())
                    suggestions = response.tags
                } catch (e: Exception) {
                    suggestions = emptyList()
                }
                isLoadingSuggestions = false
            }
    }

    // 清空输入时清空联想
    LaunchedEffect(query) {
        if (query.isBlank()) {
            suggestions = emptyList()
        }
    }

    // 返回键处理
    fun handleBack() {
        if (query.isNotEmpty()) {
            query = ""
            suggestions = emptyList()
        } else {
            navViewModel.goBack()
        }
    }

    BackHandler {
        handleBack()
    }

    // 执行搜索：记录历史并跳转到 TagDetail
    fun performSearch(tag: Tag) {
        if (tag.name.isNullOrBlank()) return
        BrowseHistoryRepository.recordSearch(tag)
        navViewModel.navigate(NavRoute.TagDetail(tag = tag))
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
                        val trimmed = query.trim()
                        if (trimmed.isNotEmpty()) {
                            performSearch(Tag(name = trimmed, translated_name = trimmed))
                        }
                    },
                    expanded = true,
                    onExpandedChange = { if (!it) handleBack() },
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = {
                        IconButton(onClick = { navViewModel.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                suggestions = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) handleBack() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            // 有联想结果时显示联想列表
            if (suggestions.isNotEmpty()) {
                SuggestionsList(
                    suggestions = suggestions,
                    keyword = query.trim(),
                    onSuggestionClick = { tag ->
                        query = tag.name ?: ""
                        performSearch(tag)
                    }
                )
            } else {
                // 无联想时显示搜索历史
                SearchHistoryContent(
                    searchHistory = searchHistory,
                    onHistoryClick = { tag ->
                        query = tag.name ?: ""
                        performSearch(tag)
                    },
                    onDeleteClick = { tagName ->
                        BrowseHistoryRepository.deleteSearchTag(tagName)
                    }
                )
            }
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<Tag>,
    keyword: String,
    onSuggestionClick: (Tag) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(suggestions, key = { it.name ?: it.hashCode() }) { tag ->
            SuggestionItem(
                tag = tag,
                keyword = keyword,
                onClick = { onSuggestionClick(tag) }
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    tag: Tag,
    keyword: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            // 高亮匹配的关键词
            HighlightedText(
                text = tag.name ?: "",
                keyword = keyword,
                style = MaterialTheme.typography.bodyLarge,
                highlightColor = MaterialTheme.colorScheme.primary
            )
        },
        supportingContent = if (tag.translated_name != null && tag.translated_name != tag.name) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.translated_name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else null,
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Tag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun HighlightedText(
    text: String,
    keyword: String,
    style: androidx.compose.ui.text.TextStyle,
    highlightColor: Color
) {
    val annotatedString = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerKeyword = keyword.lowercase()
        var currentIndex = 0

        while (currentIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerKeyword, currentIndex)
            if (matchIndex == -1) {
                // 没有更多匹配，添加剩余文本
                append(text.substring(currentIndex))
                break
            } else {
                // 添加匹配前的文本
                if (matchIndex > currentIndex) {
                    append(text.substring(currentIndex, matchIndex))
                }
                // 添加高亮的匹配文本
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                    append(text.substring(matchIndex, matchIndex + keyword.length))
                }
                currentIndex = matchIndex + keyword.length
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistoryContent(
    searchHistory: List<Tag>,
    onHistoryClick: (Tag) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (searchHistory.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchHistory.forEach { tag ->
                    val displayText = if (tag.translated_name != null && tag.translated_name != tag.name) {
                        "${tag.name} / ${tag.translated_name}"
                    } else {
                        tag.name ?: ""
                    }
                    FilterChip(
                        selected = false,
                        onClick = { onHistoryClick(tag) },
                        label = { Text(displayText) },
                        trailingIcon = {
                            IconButton(
                                onClick = { tag.name?.let { onDeleteClick(it) } },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                }
            }
        } else {
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
                        text = stringResource(R.string.search_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
