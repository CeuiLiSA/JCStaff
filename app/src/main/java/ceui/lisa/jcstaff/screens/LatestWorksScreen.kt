package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.home.LatestIllustsViewModel
import ceui.lisa.jcstaff.home.LatestNovelsViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestWorksScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_latest_illust),
        stringResource(R.string.tab_latest_manga),
        stringResource(R.string.tab_latest_novel)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.latest_works)) },
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
                modifier = Modifier.fillMaxWidth(),
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
                tabs.forEachIndexed { index, title ->
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
                        // Latest Illustrations
                        val vm: LatestIllustsViewModel = viewModel(
                            key = "latest_illust_screen",
                            factory = LatestIllustsViewModel.factory("illust")
                        )
                        val state by vm.state.collectAsState()
                        IllustGrid(
                            state = state,
                            onRefresh = { vm.refresh() },
                            onLoadMore = { vm.loadMore() }
                        )
                    }

                    1 -> {
                        // Latest Manga
                        val vm: LatestIllustsViewModel = viewModel(
                            key = "latest_manga_screen",
                            factory = LatestIllustsViewModel.factory("manga")
                        )
                        val state by vm.state.collectAsState()
                        IllustGrid(
                            state = state,
                            onRefresh = { vm.refresh() },
                            onLoadMore = { vm.loadMore() }
                        )
                    }

                    2 -> {
                        // Latest Novels
                        val vm: LatestNovelsViewModel = viewModel()
                        val state by vm.state.collectAsState()
                        NovelList(
                            state = state,
                            onRefresh = { vm.refresh() },
                            onLoadMore = { vm.loadMore() }
                        )
                    }
                }
            }
        }
    }
}
