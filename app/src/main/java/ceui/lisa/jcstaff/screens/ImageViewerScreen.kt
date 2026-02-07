package ceui.lisa.jcstaff.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.downloadToGallery
import ceui.lisa.jcstaff.core.saveFromCacheToGallery
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import androidx.compose.runtime.collectAsState
import ceui.lisa.jcstaff.core.LoadTaskManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
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
    sharedElementKey: String
) {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val effectiveUrl = originalUrl ?: imageUrl
    var isSaving by remember { mutableStateOf(false) }

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

    // 长按保存
    val saveToGallery: () -> Unit = {
        if (!isSaving) {
            isSaving = true
            coroutineScope.launch {
                val fileName = "pixiv_${System.currentTimeMillis()}"
                val cached = LoadTaskManager.getCachedFilePath(effectiveUrl)
                val result = if (cached != null) {
                    saveFromCacheToGallery(context, cached, fileName)
                } else {
                    downloadToGallery(context, effectiveUrl, fileName)
                }
                isSaving = false
                val message = if (result.isSuccess) {
                    context.getString(R.string.saved_to_gallery)
                } else {
                    context.getString(R.string.save_failed)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片容器
        Box(modifier = Modifier.fillMaxSize()) {
            // 预览图（作为底层，在原图加载完成前显示）
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { saveToGallery() })
                    }
            )

            // 原图（当下载完成后，使用缓存文件加载可缩放图片）
            if (isTaskCompleted && cachedFilePath != null) {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(cachedFilePath))
                        .build(),
                    contentDescription = null,
                    state = zoomableState,
                    modifier = Modifier.fillMaxSize(),
                    onLongClick = { saveToGallery() }
                )
            }
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
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        IconButton(
            onClick = { navViewModel.goBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 4.dp,
                    start = 4.dp
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = Color.White
            )
        }
    }
}
