package ceui.lisa.jcstaff.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.home.RankingViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import kotlinx.coroutines.launch
import java.util.Calendar

data class RankingMode(
    val mode: String,
    val labelResId: Int
)

private val illustRankingModes = listOf(
    RankingMode("day", R.string.rank_day),
    RankingMode("week", R.string.rank_week),
    RankingMode("month", R.string.rank_month),
    RankingMode("day_ai", R.string.rank_day_ai),
    RankingMode("day_male", R.string.rank_day_male),
    RankingMode("day_female", R.string.rank_day_female),
    RankingMode("week_original", R.string.rank_week_original),
    RankingMode("week_rookie", R.string.rank_week_rookie),
)

private val mangaRankingModes = listOf(
    RankingMode("day_manga", R.string.rank_day_manga),
    RankingMode("week_manga", R.string.rank_week_manga),
    RankingMode("month_manga", R.string.rank_month_manga),
    RankingMode("week_rookie_manga", R.string.rank_week_rookie_manga),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetailScreen(
    objectType: String
) {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val modes = if (objectType == "manga") mangaRankingModes else illustRankingModes
    val pagerState = rememberPagerState(pageCount = { modes.size })
    val coroutineScope = rememberCoroutineScope()
    val selectionManager = rememberSelectionManager()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.ranking)) },
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
                    // 排行榜最新数据是昨天
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            // 通知所有可见的 RankingViewModel 更新日期
                            // 通过重建 pager 实现
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    // 最小日期：2008-08-01
                    val minCal = Calendar.getInstance().apply {
                        set(2008, Calendar.AUGUST, 1)
                    }
                    dialog.datePicker.minDate = minCal.timeInMillis
                    // 最大日期：昨天
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
            edgePadding = 12.dp
        ) {
            modes.forEachIndexed { index, rankingMode ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(stringResource(rankingMode.labelResId)) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mode = modes[page]
            val rankingViewModel: RankingViewModel = viewModel(
                key = "ranking_${mode.mode}",
                factory = RankingViewModel.factory(mode.mode)
            )
            val rankingState by rankingViewModel.state.collectAsState()

            IllustGrid(
                illusts = rankingState.illusts,
                onIllustClick = { illust ->
                    navViewModel.navigate(NavRoute.IllustDetail(
                        illustId = illust.id,
                        title = illust.title ?: "",
                        previewUrl = illust.previewUrl(),
                        aspectRatio = illust.aspectRatio()
                    ))
                },
                isLoading = rankingState.isLoading,
                isLoadingMore = rankingState.isLoadingMore,
                canLoadMore = rankingState.canLoadMore,
                error = rankingState.error,
                onRefresh = { rankingViewModel.refresh() },
                onLoadMore = { rankingViewModel.loadMore() },
                selectionManager = selectionManager
            )
        }
    }
}
