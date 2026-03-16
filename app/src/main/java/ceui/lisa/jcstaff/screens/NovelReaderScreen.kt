package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Novel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    novelId: Long,
    novelTitle: String
) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: NovelReaderViewModel = viewModel(
        key = "NovelReader_$novelId",
        factory = NovelReaderViewModel.factory(novelId)
    )
    val state by viewModel.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val scrollState = rememberScrollState()
    var showSettings by remember { mutableStateOf(false) }

    // Scrollbar color
    val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    // Scroll progress for scrollbar
    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else 0f
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = statusBarPadding.calculateTopPadding())
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navViewModel.goBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = novelTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.novel_settings)
                )
            }
        }

        // Content
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.novel_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.loadNovelText() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.novel_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.novelText != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        NovelTextContent(
                            text = state.novelText!!,
                            fontSize = state.fontSize,
                            lineHeightMultiplier = state.lineHeightMultiplier,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Series navigation — horizontal
                        if (state.seriesPrev != null || state.seriesNext != null) {
                            SeriesNavigation(
                                prev = state.seriesPrev,
                                next = state.seriesNext,
                                onNavigate = { novel ->
                                    navViewModel.navigate(
                                        NavRoute.NovelReader(
                                            novelId = novel.id,
                                            novelTitle = novel.title ?: ""
                                        )
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }

                    // Scrollbar
                    if (scrollState.maxValue > 0) {
                        val scrollbarHeight = 48.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxHeight()
                                .width(6.dp)
                                .padding(vertical = 4.dp, horizontal = 1.dp)
                                .drawWithContent {
                                    val trackHeight = size.height
                                    val thumbHeight = scrollbarHeight.toPx()
                                    val thumbY =
                                        scrollProgress * (trackHeight - thumbHeight)
                                    drawRoundRect(
                                        color = scrollbarColor,
                                        topLeft = Offset(0f, thumbY),
                                        size = Size(size.width, thumbHeight),
                                        cornerRadius = CornerRadius(size.width / 2f)
                                    )
                                }
                        )
                    }
                }
            }
        }
    }

    // Settings bottom sheet
    if (showSettings) {
        ReaderSettingsSheet(
            fontSize = state.fontSize,
            lineHeightMultiplier = state.lineHeightMultiplier,
            onFontSizeChange = { viewModel.setFontSize(it) },
            onLineHeightChange = { viewModel.setLineHeight(it) },
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    fontSize: Int,
    lineHeightMultiplier: Float,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Local slider states — only commit to ViewModel on finger up
    var localFontSize by remember(fontSize) { mutableStateOf(fontSize.toFloat()) }
    var localLineHeight by remember(lineHeightMultiplier) { mutableStateOf(lineHeightMultiplier) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = navBarPadding.calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = stringResource(R.string.novel_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview text — at top for immediate visual feedback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val previewFontSize = localFontSize.roundToInt()
                Text(
                    text = "预览文字效果\nこれはプレビューです\nPreview Text",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = previewFontSize.sp,
                        lineHeight = (previewFontSize * localLineHeight).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.novel_font_size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(48.dp)
                )
                Slider(
                    value = localFontSize,
                    onValueChange = { localFontSize = it },
                    onValueChangeFinished = { onFontSizeChange(localFontSize.roundToInt()) },
                    valueRange = 12f..28f,
                    steps = 7,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${localFontSize.roundToInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line height
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.novel_line_height),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(48.dp)
                )
                Slider(
                    value = localLineHeight,
                    onValueChange = { localLineHeight = (it * 10).roundToInt() / 10f },
                    onValueChangeFinished = { onLineHeightChange(localLineHeight) },
                    valueRange = 1.2f..3.0f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${localLineHeight}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

/**
 * Renders pixiv novel text with support for special markup:
 * [newpage] — page break
 * [chapter:title] — chapter heading
 * [[rb:text > ruby]] — ruby text (furigana)
 * [pixivimage:id] — embedded image (shown as placeholder)
 * [jump:page] — page jump link
 */
@Composable
private fun NovelTextContent(
    text: String,
    fontSize: Int,
    lineHeightMultiplier: Float,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineHeightMultiplier).sp,
        letterSpacing = 0.3.sp
    )
    val paragraphs = text.split("[newpage]")

    Column(modifier = modifier) {
        paragraphs.forEachIndexed { pageIndex, page ->
            if (pageIndex > 0) {
                // Page separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            val lines = page.split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.isEmpty() -> {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    trimmed.startsWith("[chapter:") && trimmed.endsWith("]") -> {
                        val title = trimmed
                            .removePrefix("[chapter:")
                            .removeSuffix("]")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    trimmed.startsWith("[pixivimage:") && trimmed.endsWith("]") -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[ 插图 ]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        val displayText = processRubyText(trimmed)
                        Text(
                            text = displayText,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Process ruby text markup: [[rb:base > ruby]] → base(ruby)
 * Also handles [jump:N] by stripping it
 */
private fun processRubyText(text: String): String {
    var result = text

    val rubyPattern = Regex("""\[\[rb:\s*(.+?)\s*>\s*(.+?)\s*]]""")
    result = rubyPattern.replace(result) { match ->
        val base = match.groupValues[1]
        val ruby = match.groupValues[2]
        "$base($ruby)"
    }

    val jumpPattern = Regex("""\[jump:\d+]""")
    result = jumpPattern.replace(result, "")

    return result
}

@Composable
private fun SeriesNavigation(
    prev: Novel?,
    next: Novel?,
    onNavigate: (Novel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (prev != null) {
            FilledTonalButton(
                onClick = { onNavigate(prev) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = prev.title ?: "前一篇",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (next != null) {
            FilledTonalButton(
                onClick = { onNavigate(next) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = next.title ?: "后一篇",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
