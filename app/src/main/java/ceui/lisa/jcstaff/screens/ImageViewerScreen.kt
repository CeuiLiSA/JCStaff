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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import ceui.lisa.jcstaff.core.LoadTaskManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import java.io.File

/**
 * 全屏图片查看器
 * 使用 LoadTaskManager 自己维护 OkHttp 下载，与一级详情页共享：
 * - 从详情页点击进入时，复用已有的加载任务和缓存文件
 * - 退出再进入时，续上上一个请求而不是新发请求
 * - 下载完成后直接使用缓存文件，点击下载按钮瞬间完成
 */
@Composable
fun ImageViewerScreen(
    imageUrl: String,
    originalUrl: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val effectiveUrl = originalUrl ?: imageUrl

    // 使用 LoadTaskManager 管理加载任务（与一级详情页共享）
    // registerListener 会自动启动下载任务
    val loadTaskFlow = remember(effectiveUrl) {
        LoadTaskManager.registerListener(effectiveUrl)
    }
    val loadTask by loadTaskFlow.collectAsState()

    // 从任务状态中获取进度
    val downloadProgress = loadTask.progress
    val isTaskLoading = loadTask.isLoading
    val isTaskCompleted = loadTask.isCompleted
    val cachedFilePath = loadTask.cachedFilePath

    // 页面退出时取消监听（但不取消下载任务本身）
    DisposableEffect(effectiveUrl) {
        onDispose {
            LoadTaskManager.unregisterListener(effectiveUrl)
        }
    }

    val zoomableState = rememberZoomableImageState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 预览图（作为底层，在原图加载完成前显示）
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // 原图（当下载完成后，使用缓存文件加载可缩放图片）
        if (isTaskCompleted && cachedFilePath != null) {
            ZoomableAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(cachedFilePath))
                    .build(),
                contentDescription = null,
                state = zoomableState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载进度指示器
        if (isTaskLoading && !isTaskCompleted) {
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
