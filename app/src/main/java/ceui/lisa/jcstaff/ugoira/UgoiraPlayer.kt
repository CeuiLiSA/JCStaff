package ceui.lisa.jcstaff.ugoira

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Ugoira 播放器组件
 *
 * @param illustId 作品 ID
 * @param previewUrl 预览图 URL（加载时显示）
 * @param aspectRatio 宽高比
 * @param modifier Modifier
 * @param viewModel ViewModel
 */
@Composable
fun UgoiraPlayer(
    illustId: Long,
    previewUrl: String,
    aspectRatio: Float = 1f,
    modifier: Modifier = Modifier,
    viewModel: UgoiraViewModel = viewModel(key = "ugoira_$illustId")
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // 启动加载
    LaunchedEffect(illustId) {
        viewModel.load(context, illustId)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (val currentState = state) {
            is UgoiraState.Idle,
            is UgoiraState.FetchingMetadata -> {
                PreviewWithOverlay(previewUrl, "获取元数据...")
            }

            is UgoiraState.Downloading -> {
                PreviewWithProgress(previewUrl, "下载中", currentState.progress)
            }

            is UgoiraState.Extracting -> {
                PreviewWithOverlay(previewUrl, "解压中...")
            }

            is UgoiraState.Encoding -> {
                PreviewWithOverlay(previewUrl, "处理中...")
            }

            is UgoiraState.Done -> {
                UgoiraAnimation(frames = currentState.frames)
            }

            is UgoiraState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.retry(context) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun UgoiraAnimation(frames: UgoiraFrames) {
    val context = LocalContext.current
    var currentTime by remember { mutableLongStateOf(0L) }

    // 动画循环
    LaunchedEffect(frames) {
        val startTime = System.currentTimeMillis()
        while (isActive) {
            currentTime = System.currentTimeMillis() - startTime
            delay(16) // ~60fps
        }
    }

    val currentFrame = remember(currentTime, frames) {
        frames.getFrameAtTime(currentTime)
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(currentFrame)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PreviewWithOverlay(previewUrl: String, text: String) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        LoadingOverlay(text = text)
    }
}

@Composable
private fun PreviewWithProgress(previewUrl: String, text: String, progress: Int) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        LoadingOverlay(text = "$text $progress%", progress = progress / 100f)
    }
}

@Composable
private fun LoadingOverlay(
    text: String,
    progress: Float? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
