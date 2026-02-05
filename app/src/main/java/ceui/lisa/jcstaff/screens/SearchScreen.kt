package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.TrendingTag

// 标签渐变色（与 IllustTags 保持一致）
private val tagGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),
    listOf(Color(0xFF4ECDC4), Color(0xFF44A08D)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF5EE7DF), Color(0xFFB490CA)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF8E9EAB), Color(0xFFEEF2F3)),
    listOf(Color(0xFF3A6186), Color(0xFF89253E)),
    listOf(Color(0xFF56AB2F), Color(0xFFA8E063)),
    listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)),
    listOf(Color(0xFFCC2B5E), Color(0xFF753A88)),
    listOf(Color(0xFFED4264), Color(0xFFFFEDBC)),
    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
)

/**
 * 搜索页面
 * 支持关键词联想（debounce 500ms）和搜索历史
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current

    // 从 ViewModel 获取状态
    val searchHistoryState by viewModel.searchHistoryState.collectAsState()
    val suggestionsState by viewModel.suggestionsState.collectAsState()
    val trendingTagsState by viewModel.trendingTagsState.collectAsState()
    val queryText by viewModel.queryText.collectAsState()

    // 使用 TextFieldValue 以支持光标位置控制
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(queryText, TextRange(queryText.length)))
    }

    val focusRequester = remember { FocusRequester() }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    // 同步 ViewModel 的 queryText 到 textFieldValue（仅当外部更新时）
    LaunchedEffect(queryText) {
        if (textFieldValue.text != queryText) {
            textFieldValue = TextFieldValue(queryText, TextRange(queryText.length))
        }
    }

    // 首次进入时延迟请求焦点，避免卡顿
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    // 更新文本的辅助函数
    fun updateQuery(newText: String) {
        viewModel.updateQuery(newText)
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
    }

    BackHandler {
        if (queryText.isNotEmpty()) {
            updateQuery("")
        } else {
            navViewModel.goBack()
        }
    }

    // 执行搜索：记录历史并跳转到 TagDetail
    fun performSearch(tag: Tag) {
        if (tag.name.isNullOrBlank()) return
        viewModel.recordSearch(tag)
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
                viewModel.updateQuery(newValue.text)
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
        if (suggestionsState.suggestions.isNotEmpty()) {
            SuggestionsList(
                suggestions = suggestionsState.suggestions,
                keyword = queryText.trim(),
                isLoading = suggestionsState.isLoading,
                onSuggestionClick = { tag ->
                    updateQuery(tag.name ?: "")
                    performSearch(tag)
                }
            )
        } else {
            SearchHistoryContent(
                searchHistoryState = searchHistoryState,
                trendingTagsState = trendingTagsState,
                onHistoryClick = { tag ->
                    updateQuery(tag.name ?: "")
                    performSearch(tag)
                },
                onTrendingClick = { trendingTag ->
                    val tag = Tag(
                        name = trendingTag.tag,
                        translated_name = trendingTag.translated_name
                    )
                    // 记录搜索历史并跳转到网页版标签详情
                    viewModel.recordSearch(tag)
                    navViewModel.navigate(NavRoute.WebTagDetail(tag))
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
                            tag.name?.let { viewModel.deleteSearchTag(it) }
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
    isLoading: Boolean,
    onSuggestionClick: (Tag) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
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
        // 加载指示器
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(20.dp),
                strokeWidth = 2.dp
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

private const val HISTORY_COLLAPSED_COUNT = 10

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistoryContent(
    searchHistoryState: SearchHistoryState,
    trendingTagsState: TrendingTagsState,
    onHistoryClick: (Tag) -> Unit,
    onTrendingClick: (TrendingTag) -> Unit,
    onDeleteClick: (Tag) -> Unit
) {
    val searchHistory = searchHistoryState.history
    val trendingTags = trendingTagsState.tags
    var isHistoryExpanded by remember { mutableStateOf(false) }

    // 根据展开状态决定显示的历史记录
    val displayedHistory = if (isHistoryExpanded || searchHistory.size <= HISTORY_COLLAPSED_COUNT) {
        searchHistory
    } else {
        searchHistory.take(HISTORY_COLLAPSED_COUNT)
    }
    val hasMoreHistory = searchHistory.size > HISTORY_COLLAPSED_COUNT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 最近搜索
        if (searchHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_searches),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 展开/收起按钮
                if (hasMoreHistory) {
                    Text(
                        text = if (isHistoryExpanded) {
                            stringResource(R.string.collapse_images)
                        } else {
                            stringResource(R.string.expand_images)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isHistoryExpanded = !isHistoryExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedHistory.forEachIndexed { index, tag ->
                    SearchTagChip(
                        tag = tag,
                        gradientColors = tagGradients[index % tagGradients.size],
                        onClick = { onHistoryClick(tag) },
                        onDeleteClick = { onDeleteClick(tag) }
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trendingTags.forEachIndexed { index, trendingTag ->
                    val tag = Tag(
                        name = trendingTag.tag,
                        translated_name = trendingTag.translated_name
                    )
                    SearchTagChip(
                        tag = tag,
                        gradientColors = tagGradients[(index + 5) % tagGradients.size],
                        onClick = { onTrendingClick(trendingTag) }
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

/**
 * 搜索标签芯片 - 带渐变背景
 */
@Composable
private fun SearchTagChip(
    tag: Tag,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val cornerRadius = 8.dp

    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        gradientColors[0].copy(alpha = 0.15f),
                        gradientColors[1].copy(alpha = 0.1f)
                    )
                )
            )
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "#${tag.name ?: ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = gradientColors[0],
                maxLines = 1
            )
            tag.translated_name?.takeIf { it != tag.name }?.let { translated ->
                Text(
                    text = translated,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }

        // 删除按钮（仅搜索历史显示）
        if (onDeleteClick != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDeleteClick() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
