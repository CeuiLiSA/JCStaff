package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.core.NovelListViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelSeriesScreen(
    seriesId: Long,
    seriesTitle: String
) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: NovelListViewModel = viewModel(
        key = "novel_series_$seriesId",
        factory = NovelListViewModel.factory(
            loadFirstPage = { PixivClient.pixivApi.getNovelSeries(seriesId) }
        )
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seriesTitle.ifEmpty { "系列" }) },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        NovelList(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() }
        )
    }
}
