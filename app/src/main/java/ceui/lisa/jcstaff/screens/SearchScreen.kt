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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.TrendingTag
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
    val searchHistory by BrowseHistoryRepository.getSearchHistoryFlow()
        .collectAsState(initial = emptyList())
    // 使用 TextFieldValue 以支持光标位置控制
    var queryText by rememberSaveable { mutableStateOf("") }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                queryText,
                TextRange(queryText.length)
            )
        )
    }
    var suggestions by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var trendingTags by remember { mutableStateOf<List<TrendingTag>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var shouldFocus by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    // 监听生命周期，在页面恢复时（如从 TagDetail 返回）将光标定位到文字末尾
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                textFieldValue = TextFieldValue(queryText, TextRange(queryText.length))
                shouldFocus = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 在 Compose 上下文中执行 focus 请求
    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            focusRequester.requestFocus()
            shouldFocus = false
        }
    }

    // Debounce 关键词联想
    LaunchedEffect(Unit) {
        snapshotFlow { queryText }
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
    LaunchedEffect(queryText) {
        if (queryText.isBlank()) {
            suggestions = emptyList()
        }
    }

    // 加载热门标签
    LaunchedEffect(Unit) {
        try {
            val response = PixivClient.pixivApi.getTrendingTags()
            trendingTags = response.trend_tags
        } catch (e: Exception) {
            // ignore
        }
    }

    // 更新文本的辅助函数
    fun updateQuery(newText: String) {
        queryText = newText
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
    }

    BackHandler {
        if (queryText.isNotEmpty()) {
            updateQuery("")
            suggestions = emptyList()
        } else {
            navViewModel.goBack()
        }
    }

    // 执行搜索：记录历史并跳转到 TagDetail
    fun performSearch(tag: Tag) {
        if (tag.name.isNullOrBlank()) return
        BrowseHistoryRepository.recordSearch(tag)
        navViewModel.navigate(NavRoute.TagDetail(tag = tag))
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部标题栏
        TopAppBar(
            title = { Text(stringResource(R.string.search)) },
            navigationIcon = {
                IconButton(onClick = { navViewModel.goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        // 搜索输入框
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                queryText = newValue.text
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (queryText.isNotEmpty()) {
                    IconButton(onClick = {
                        updateQuery("")
                        suggestions = emptyList()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val trimmed = queryText.trim()
                    if (trimmed.isNotEmpty()) {
                        performSearch(Tag(name = trimmed, translated_name = trimmed))
                    }
                }
            ),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        // 内容区域
        if (suggestions.isNotEmpty()) {
            SuggestionsList(
                suggestions = suggestions,
                keyword = queryText.trim(),
                onSuggestionClick = { tag ->
                    updateQuery(tag.name ?: "")
                    performSearch(tag)
                }
            )
        } else {
            SearchHistoryContent(
                searchHistory = searchHistory,
                trendingTags = trendingTags,
                onHistoryClick = { tag ->
                    updateQuery(tag.name ?: "")
                    performSearch(tag)
                },
                onTrendingClick = { trendingTag ->
                    val tag = Tag(
                        name = trendingTag.tag,
                        translated_name = trendingTag.translated_name
                    )
                    updateQuery(trendingTag.tag ?: "")
                    performSearch(tag)
                },
                onDeleteClick = { tag ->
                    tagToDelete = tag
                }
            )
        }

        // 删除确认弹窗
        tagToDelete?.let { tag ->
            AlertDialog(
                onDismissRequest = { tagToDelete = null },
                title = {
                    Text(stringResource(R.string.delete_search_history_title))
                },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_search_history_message,
                            tag.name ?: ""
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            tag.name?.let { BrowseHistoryRepository.deleteSearchTag(it) }
                            tagToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tagToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
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
    // 空关键词直接显示原文本，避免无限循环
    if (keyword.isEmpty()) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

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
    trendingTags: List<TrendingTag>,
    onHistoryClick: (Tag) -> Unit,
    onTrendingClick: (TrendingTag) -> Unit,
    onDeleteClick: (Tag) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 最近搜索
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
                    val displayText =
                        if (tag.translated_name != null && tag.translated_name != tag.name) {
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
                                onClick = { onDeleteClick(tag) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.delete),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 热门搜索
        if (trendingTags.isNotEmpty()) {
            Text(
                text = stringResource(R.string.trending_searches),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trendingTags.forEach { trendingTag ->
                    val displayText =
                        if (trendingTag.translated_name != null && trendingTag.translated_name != trendingTag.tag) {
                            "${trendingTag.tag} / ${trendingTag.translated_name}"
                        } else {
                            trendingTag.tag ?: ""
                        }
                    FilterChip(
                        selected = false,
                        onClick = { onTrendingClick(trendingTag) },
                        label = { Text(displayText) }
                    )
                }
            }
        }

        // 无内容时的空状态
        if (searchHistory.isEmpty() && trendingTags.isEmpty()) {
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
