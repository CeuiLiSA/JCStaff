package ceui.lisa.jcstaff.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.LoadTaskManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.saket.telephoto.zoomable.rememberZoomablePeekOverlayState
import me.saket.telephoto.zoomable.zoomablePeekOverlay
import java.io.File

/**
 * 渐进式图片加载组件（支持 Peek Overlay 缩放）
 * 使用 LoadTaskManager 自己维护 OkHttp 下载，支持：
 * - 一级详情页和二级详情页共享进度
 * - 退出再进入时续上上一个请求
 * - 下载完成后保存到缓存文件
 * - 原图下载完成后支持 Peek Overlay 手势缩放预览
 */
@Composable
fun ProgressiveImage(
    previewUrl: String,
    originalUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Peek Overlay 状态（用于手势缩放预览）
    val peekOverlayState = rememberZoomablePeekOverlayState()

    // 使用 LoadTaskManager 管理加载任务（自己维护 OkHttp 下载）
    // registerListener 会自动启动下载任务
    val loadTaskFlow = remember(originalUrl) {
        originalUrl?.let { LoadTaskManager.registerListener(it) }
    }
    val loadTask by loadTaskFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    // 从任务状态中获取进度和加载状态
    val downloadProgress = loadTask?.progress ?: 0f
    val isTaskLoading = loadTask?.isLoading == true
    val isTaskCompleted = loadTask?.isCompleted == true
    val cachedFilePath = loadTask?.cachedFilePath

    // 页面退出时取消监听（但不取消下载任务本身）
    DisposableEffect(originalUrl) {
        onDispose {
            originalUrl?.let { LoadTaskManager.unregisterListener(it) }
        }
    }

    Box(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        // 图片容器
        Box(modifier = Modifier.fillMaxSize()) {
            // 预览图（始终显示作为底层）
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )

            // 原图（当下载完成后，从缓存文件加载，支持 Peek Overlay 缩放预览）
            if (originalUrl != null && originalUrl != previewUrl && isTaskCompleted && cachedFilePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(cachedFilePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomablePeekOverlay(peekOverlayState)
                )
            }
        }

        // 原图加载中的进度指示器（带百分比）
        if (originalUrl != null && originalUrl != previewUrl && isTaskLoading && !isTaskCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.Transparent
                )
                // 进度圆环
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                    trackColor = Color.Transparent
                )
                // 百分比文字
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
