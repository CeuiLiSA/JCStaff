package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.ProgressManager
import ceui.lisa.jcstaff.core.createProgressImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

@Composable
fun ImageViewerScreen(
    imageUrl: String,
    originalUrl: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val effectiveUrl = originalUrl ?: imageUrl

    var isLoading by remember(effectiveUrl) { mutableStateOf(false) }
    var isLoaded by remember(effectiveUrl) { mutableStateOf(false) }
    var downloadProgress by remember(effectiveUrl) { mutableStateOf(0f) }

    // 创建带进度追踪的 ImageLoader
    val progressImageLoader = remember(context) { createProgressImageLoader(context) }

    // 监听下载进度
    DisposableEffect(effectiveUrl) {
        ProgressManager.addListener(effectiveUrl) { _, bytesRead, contentLength ->
            if (contentLength > 0) {
                downloadProgress = bytesRead.toFloat() / contentLength.toFloat()
            }
        }
        onDispose {
            ProgressManager.removeListener(effectiveUrl)
        }
    }

    val zoomableState = rememberZoomableImageState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 隐藏的 AsyncImage 用于追踪加载状态
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(effectiveUrl)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            imageLoader = progressImageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        isLoading = true
                    }
                    is AsyncImagePainter.State.Success -> {
                        isLoading = false
                        isLoaded = true
                    }
                    is AsyncImagePainter.State.Error -> {
                        isLoading = false
                    }
                    else -> {}
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f) // 隐藏，仅用于状态追踪
        )

        // 可缩放的图片
        ZoomableAsyncImage(
            model = ImageRequest.Builder(context)
                .data(effectiveUrl)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            imageLoader = progressImageLoader,
            contentDescription = null,
            state = zoomableState,
            modifier = Modifier.fillMaxSize()
        )

        // 加载进度指示器（与一级详情页一致）
        if (isLoading && !isLoaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.Transparent
                )
                // 进度圆环
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp,
                    color = Color.White,
                    trackColor = Color.Transparent
                )
                // 百分比文字
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 关闭按钮
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
    }
}
