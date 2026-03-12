package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * 通用空态组件（简单版，用于内嵌预览行等场景）
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.no_content)
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_empty))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(120.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 全屏空态组件，支持滚动（适配 PullToRefreshBox）和刷新按钮
 * 用于列表/网格加载完成但无数据的场景
 */
@Composable
fun EmptyRefreshableState(
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.no_content)
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_empty))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRefresh != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) {
                Text(stringResource(R.string.refresh))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
