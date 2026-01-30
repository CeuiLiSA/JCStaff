package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R

/**
 * 通用错误重试状态组件
 *
 * 支持两种模式：
 * 1. 可滚动模式 (scrollable = true): 用于被 PullToRefreshBox 包裹的场景，支持下拉刷新
 * 2. 普通模式 (scrollable = false): 用于不需要下拉刷新的场景
 *
 * @param error 错误信息
 * @param onRetry 重试回调
 * @param modifier Modifier
 * @param scrollable 是否支持滚动（用于 PullToRefreshBox 下拉刷新检测）
 * @param showPullToRefreshHint 是否显示"下拉刷新重试"提示
 */
@Composable
fun ErrorRetryState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    showPullToRefreshHint: Boolean = true
) {
    val columnModifier = if (scrollable) {
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    } else {
        modifier.fillMaxSize()
    }

    Column(
        modifier = columnModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (scrollable) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = error,
            color = MaterialTheme.colorScheme.error
        )

        if (showPullToRefreshHint) {
            Text(
                text = stringResource(R.string.pull_to_refresh_retry),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }

        if (scrollable) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
