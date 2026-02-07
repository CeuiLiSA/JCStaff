package ceui.lisa.jcstaff.ugoira

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

/**
 * Ugoira player component
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

    LaunchedEffect(illustId) {
        viewModel.load(context, illustId)
    }

    // ImageLoader with GIF support — remember to avoid recreating on every recomposition
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (val currentState = state) {
            is UgoiraState.Idle,
            is UgoiraState.FetchingMetadata -> {
                PreviewWithOverlay(previewUrl, stringResource(R.string.ugoira_fetching_metadata))
            }

            is UgoiraState.Downloading -> {
                val text = "${stringResource(R.string.ugoira_downloading)} ${currentState.progress}%"
                PreviewWithProgress(previewUrl, text, currentState.progress)
            }

            is UgoiraState.Extracting -> {
                PreviewWithOverlay(previewUrl, stringResource(R.string.ugoira_extracting))
            }

            is UgoiraState.Encoding -> {
                val text = "${stringResource(R.string.ugoira_encoding)} ${currentState.progress}%"
                PreviewWithProgress(previewUrl, text, currentState.progress)
            }

            is UgoiraState.Done -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentState.data.gifFile)
                        .crossfade(false)
                        .build(),
                    imageLoader = gifImageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is UgoiraState.Error -> {
                val errorText = if (currentState.errorCode != null) {
                    stringResource(currentState.errorResId, currentState.errorCode)
                } else {
                    stringResource(currentState.errorResId)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorText,
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
private fun PreviewWithOverlay(previewUrl: String, text: String) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        LoadingOverlay(
            text = text,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PreviewWithProgress(previewUrl: String, text: String, progress: Int) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        LoadingOverlay(
            text = text,
            progress = progress / 100f,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun LoadingOverlay(
    text: String,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
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
