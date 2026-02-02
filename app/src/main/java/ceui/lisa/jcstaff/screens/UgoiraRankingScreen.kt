package ceui.lisa.jcstaff.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.home.UgoiraRankingViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.PixivWebScraper
import kotlinx.coroutines.launch
import java.util.Calendar

data class UgoiraRankingMode(
    val mode: String,
    val labelResId: Int
)

private val ugoiraRankingModes = listOf(
    UgoiraRankingMode(PixivWebScraper.RankingMode.DAILY, R.string.rank_day),
    UgoiraRankingMode(PixivWebScraper.RankingMode.WEEKLY, R.string.rank_week),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UgoiraRankingScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { ugoiraRankingModes.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.ugoira_ranking)) },
            navigationIcon = {
                IconButton(onClick = { navViewModel.goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val date = String.format("%04d%02d%02d", year, month + 1, dayOfMonth)
                            // TODO: 传递日期到当前 tab 的 ViewModel
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    val minCal = Calendar.getInstance().apply {
                        set(2008, Calendar.AUGUST, 1)
                    }
                    dialog.datePicker.minDate = minCal.timeInMillis
                    val maxCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, -1)
                    }
                    dialog.datePicker.maxDate = maxCal.timeInMillis
                    dialog.show()
                }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.select_date)
                    )
                }
            }
        )

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ugoiraRankingModes.forEachIndexed { index, mode ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(stringResource(mode.labelResId)) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mode = ugoiraRankingModes[page]
            val viewModel: UgoiraRankingViewModel = viewModel(
                key = "ugoira_ranking_${mode.mode}",
                factory = UgoiraRankingViewModel.factory(mode.mode)
            )
            val state by viewModel.state.collectAsState()

            IllustGrid(
                illusts = state.illusts,
                onIllustClick = { illust ->
                    navViewModel.navigate(
                        NavRoute.IllustDetail(
                            illustId = illust.id,
                            title = illust.title ?: "",
                            previewUrl = illust.previewUrl(),
                            aspectRatio = illust.aspectRatio()
                        )
                    )
                },
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                error = state.error,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
            )
        }
    }
}
