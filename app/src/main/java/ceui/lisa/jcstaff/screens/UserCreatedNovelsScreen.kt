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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.NovelListViewModel
import ceui.lisa.jcstaff.core.NovelLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCreatedNovelsScreen(
    userId: Long
) {
    val navViewModel = LocalNavigationViewModel.current
    val viewModel: NovelListViewModel = viewModel(key = "user_created_novels_$userId")
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        val cacheConfig = CacheConfig(
            path = "/v1/user/novels",
            queryParams = mapOf("user_id" to userId.toString(), "filter" to "for_ios")
        )
        viewModel.bind(
            loader = NovelLoader {
                PixivClient.pixivApi.getUserNovels(userId)
            },
            cacheConfig = cacheConfig
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_created_novels)) },
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
        NovelList(
            state = PagedState(
                items = state.novels,
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                error = state.error,
                nextUrl = state.nextUrl
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() }
        )
    }
}
