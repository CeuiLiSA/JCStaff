package ceui.lisa.jcstaff.screens

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.manga.MangaPage
import ceui.lisa.jcstaff.manga.MangaReaderSettings
import ceui.lisa.jcstaff.manga.MangaReaderViewModel
import ceui.lisa.jcstaff.manga.PageDisplayMode
import ceui.lisa.jcstaff.manga.ReadingDirection
import ceui.lisa.jcstaff.manga.ReadingMode
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    illustId: Long,
    illustTitle: String,
    initialPage: Int = 0,
    viewModel: MangaReaderViewModel = viewModel(
        key = "manga_reader_$illustId",
        factory = MangaReaderViewModel.factory(illustId)
    )
) {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val pages by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val vmTitle by viewModel.illustTitle.collectAsState()

    val title = vmTitle.ifEmpty { illustTitle }
    val pageCount = pages.size

    var showSettings by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keep screen on while reading
    DisposableEffect(settings.keepScreenOn) {
        if (settings.keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose { view.keepScreenOn = false }
    }

    // Set initial page once pages are loaded
    LaunchedEffect(pages, initialPage) {
        if (pages.isNotEmpty() && initialPage > 0) {
            viewModel.goToPage(initialPage)
        }
    }

    // ── Pager state (for PAGED mode) ────────────────────────────────────

    // In double page mode, number of pager positions = ceil(pageCount / 2)
    val isDouble = settings.pageDisplayMode == PageDisplayMode.DOUBLE
    val pagerItemCount = when {
        pages.isEmpty() -> 0
        isDouble -> (pageCount + 1) / 2
        else -> pageCount
    }
    val pagerState = rememberPagerState(
        initialPage = if (isDouble) currentPage / 2 else currentPage,
        pageCount = { pagerItemCount }
    )

    // Sync pager ↔ ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pagerIndex ->
                val actualPage = if (isDouble) pagerIndex * 2 else pagerIndex
                viewModel.goToPage(actualPage)
            }
    }
    LaunchedEffect(currentPage, isDouble) {
        val targetPagerPage = if (isDouble) currentPage / 2 else currentPage
        if (pagerState.currentPage != targetPagerPage) {
            pagerState.animateScrollToPage(targetPagerPage)
        }
    }

    // ── Scroll state (for SCROLL mode) ──────────────────────────────────
    val scrollState = rememberLazyListState()

    LaunchedEffect(currentPage, settings.readingMode) {
        if (settings.readingMode == ReadingMode.SCROLL && pages.isNotEmpty()) {
            scrollState.animateScrollToItem(currentPage)
        }
    }

    BackHandler { navViewModel.goBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error ?: stringResource(R.string.load_error),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (pages.isNotEmpty()) {
            when (settings.readingMode) {
                ReadingMode.PAGED -> {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = settings.readingDirection == ReadingDirection.RTL,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                    ) { pagerIndex ->
                        if (isDouble) {
                            val leftPageIdx = pagerIndex * 2
                            val rightPageIdx = leftPageIdx + 1
                            val leftPage = pages.getOrNull(leftPageIdx)
                            val rightPage = pages.getOrNull(rightPageIdx)
                            DoublePageItem(
                                leftPage = leftPage,
                                rightPage = rightPage,
                                isRtl = settings.readingDirection == ReadingDirection.RTL
                            )
                        } else {
                            val page = pages[pagerIndex]
                            SinglePageItem(page = page)
                        }
                    }
                }

                ReadingMode.SCROLL -> {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pages.size) { index ->
                            val page = pages[index]
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(page.originalUrl ?: page.previewUrl)
                                    .crossfade(true)
                                    .placeholderMemoryCacheKey(page.previewUrl)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Track scroll position to update currentPage
                    LaunchedEffect(scrollState) {
                        snapshotFlow { scrollState.firstVisibleItemIndex }
                            .distinctUntilChanged()
                            .collect { index -> viewModel.goToPage(index) }
                    }
                }
            }

            // ── Tap zones for paged navigation ───────────────────────────
            if (settings.readingMode == ReadingMode.PAGED) {
                val isRtl = settings.readingDirection == ReadingDirection.RTL
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left zone
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.25f)
                            .pointerInput(isRtl) {
                                detectTapGestures {
                                    if (isRtl) viewModel.nextPage() else viewModel.prevPage()
                                }
                            }
                    )
                    // Center zone – toggle controls
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f)
                            .pointerInput(Unit) {
                                detectTapGestures { viewModel.toggleControls() }
                            }
                    )
                    // Right zone
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.25f)
                            .pointerInput(isRtl) {
                                detectTapGestures {
                                    if (isRtl) viewModel.prevPage() else viewModel.nextPage()
                                }
                            }
                    )
                }
            }
        }

        // ── Compact page indicator (always visible) ──────────────────────
        if (pages.isNotEmpty() && !showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    )
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentPage + 1} / $pageCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Top controls bar ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(top = statusBarPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Reading direction quick toggle
                    IconButton(onClick = {
                        val newDir = if (settings.readingDirection == ReadingDirection.LTR)
                            ReadingDirection.RTL else ReadingDirection.LTR
                        viewModel.updateSettings(settings.copy(readingDirection = newDir))
                    }) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.manga_toggle_direction),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        viewModel.keepControlsVisible()
                        showSettings = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.manga_settings),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ── Bottom controls bar ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls && pages.isNotEmpty(),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(bottom = navBarPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Page indicator text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev button
                        IconButton(
                            onClick = {
                                if (settings.readingDirection == ReadingDirection.RTL)
                                    viewModel.nextPage() else viewModel.prevPage()
                            },
                            enabled = if (settings.readingDirection == ReadingDirection.RTL)
                                currentPage < pageCount - 1 else currentPage > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Page text + slider
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${currentPage + 1} / $pageCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (pageCount > 1) {
                                val isRtl = settings.readingDirection == ReadingDirection.RTL
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { newVal ->
                                        viewModel.keepControlsVisible()
                                        coroutineScope.launch {
                                            val targetPage = newVal.toInt()
                                            viewModel.goToPage(targetPage)
                                            val pagerTarget = if (isDouble) targetPage / 2 else targetPage
                                            pagerState.scrollToPage(pagerTarget)
                                        }
                                    },
                                    valueRange = 0f..(pageCount - 1).toFloat(),
                                    steps = (pageCount - 2).coerceAtLeast(0),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                                )
                            }
                        }

                        // Next button
                        IconButton(
                            onClick = {
                                if (settings.readingDirection == ReadingDirection.RTL)
                                    viewModel.prevPage() else viewModel.nextPage()
                            },
                            enabled = if (settings.readingDirection == ReadingDirection.RTL)
                                currentPage > 0 else currentPage < pageCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Settings bottom sheet ────────────────────────────────────────────
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = {
                showSettings = false
                viewModel.showControls()
            },
            sheetState = settingsSheetState
        ) {
            MangaReaderSettingsSheet(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onDismiss = {
                    showSettings = false
                    viewModel.showControls()
                }
            )
        }
    }
}

// ── Single page with zoom ────────────────────────────────────────────────────

@Composable
private fun SinglePageItem(page: MangaPage) {
    val context = LocalContext.current
    val zoomableState = rememberZoomableState()
    val zoomableImageState = rememberZoomableImageState(zoomableState)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ZoomableAsyncImage(
            model = ImageRequest.Builder(context)
                .data(page.originalUrl ?: page.previewUrl)
                .placeholderMemoryCacheKey(page.previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            state = zoomableImageState,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ── Double page side-by-side ─────────────────────────────────────────────────

@Composable
private fun DoublePageItem(
    leftPage: MangaPage?,
    rightPage: MangaPage?,
    isRtl: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center
    ) {
        val first = if (isRtl) rightPage else leftPage
        val second = if (isRtl) leftPage else rightPage

        first?.let { page ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(page.originalUrl ?: page.previewUrl)
                    .placeholderMemoryCacheKey(page.previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        } ?: Spacer(modifier = Modifier.weight(1f))

        second?.let { page ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(page.originalUrl ?: page.previewUrl)
                    .placeholderMemoryCacheKey(page.previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        } ?: Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Settings sheet ───────────────────────────────────────────────────────────

@Composable
private fun MangaReaderSettingsSheet(
    settings: MangaReaderSettings,
    onSettingsChanged: (MangaReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Handle indicator
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.manga_settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ── Reading Direction ─────────────────────────────────────────
        SettingsSectionTitle(stringResource(R.string.manga_reading_direction))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(
                label = stringResource(R.string.manga_direction_ltr),
                selected = settings.readingDirection == ReadingDirection.LTR,
                onClick = { onSettingsChanged(settings.copy(readingDirection = ReadingDirection.LTR)) }
            )
            SettingsChip(
                label = stringResource(R.string.manga_direction_rtl),
                selected = settings.readingDirection == ReadingDirection.RTL,
                onClick = { onSettingsChanged(settings.copy(readingDirection = ReadingDirection.RTL)) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(20.dp))

        // ── Reading Mode ──────────────────────────────────────────────
        SettingsSectionTitle(stringResource(R.string.manga_reading_mode))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip(
                label = stringResource(R.string.manga_mode_paged),
                selected = settings.readingMode == ReadingMode.PAGED,
                onClick = { onSettingsChanged(settings.copy(readingMode = ReadingMode.PAGED)) }
            )
            SettingsChip(
                label = stringResource(R.string.manga_mode_scroll),
                selected = settings.readingMode == ReadingMode.SCROLL,
                onClick = { onSettingsChanged(settings.copy(readingMode = ReadingMode.SCROLL)) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(20.dp))

        // ── Page Display ──────────────────────────────────────────────
        if (settings.readingMode == ReadingMode.PAGED) {
            SettingsSectionTitle(stringResource(R.string.manga_page_display))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsChip(
                    label = stringResource(R.string.manga_single_page),
                    selected = settings.pageDisplayMode == PageDisplayMode.SINGLE,
                    onClick = { onSettingsChanged(settings.copy(pageDisplayMode = PageDisplayMode.SINGLE)) }
                )
                SettingsChip(
                    label = stringResource(R.string.manga_double_page),
                    selected = settings.pageDisplayMode == PageDisplayMode.DOUBLE,
                    onClick = { onSettingsChanged(settings.copy(pageDisplayMode = PageDisplayMode.DOUBLE)) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Auto-hide controls ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.manga_auto_hide_controls),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.manga_auto_hide_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.autoHideControls,
                onCheckedChange = { onSettingsChanged(settings.copy(autoHideControls = it)) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(20.dp))

        // ── Keep screen on ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.manga_keep_screen_on),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = settings.keepScreenOn,
                onCheckedChange = { onSettingsChanged(settings.copy(keepScreenOn = it)) }
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
