package ceui.lisa.jcstaff.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.FileProvider
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.downloadToGallery
import ceui.lisa.jcstaff.core.saveFromCacheToGallery
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.network.PixivClient
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

/**
 * 全屏图片查看器
 * 使用 LoadTaskManager 自己维护 OkHttp 下载，与一级详情页共享：
 * - 从详情页点击进入时，复用已有的加载任务和缓存文件
 * - 退出再进入时，续上上一个请求而不是新发请求
 * - 下载完成后直接使用缓存文件，点击下载按钮瞬间完成
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var showBottomSheet by remember { mutableStateOf(false) }

    // 使用 LoadTaskManager 管理加载任务（与一级详情页共享）
    val loadTaskFlow = remember(effectiveUrl) {
        LoadTaskManager.registerListener(effectiveUrl)
    }
    val loadTask by loadTaskFlow.collectAsState()

    val downloadProgress = loadTask.progress
    val isTaskLoading = loadTask.isLoading
    val isTaskCompleted = loadTask.isCompleted
    val cachedFilePath = loadTask.cachedFilePath

    DisposableEffect(effectiveUrl) {
        onDispose {
            LoadTaskManager.unregisterListener(effectiveUrl)
        }
    }

    val zoomableState = rememberZoomableImageState()

    val onLongPress: () -> Unit = { showBottomSheet = true }

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
                        detectTapGestures(onLongPress = { onLongPress() })
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
                    onLongClick = { onLongPress() }
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
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp,
                    color = Color.White,
                    trackColor = Color.Transparent
                )
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

    // 长按操作菜单
    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 保存到相册
                ListItem(
                    headlineContent = { Text(stringResource(R.string.save_to_gallery)) },
                    leadingContent = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) {
                            showBottomSheet = false
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
                )

                // 设为壁纸
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_as_wallpaper)) },
                    leadingContent = {
                        Icon(Icons.Default.Wallpaper, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) {
                            showBottomSheet = false
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val imageFile = prepareShareableImageFile(context, effectiveUrl)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    val mimeType = when {
                                        imageFile.name.endsWith(".png", true) -> "image/png"
                                        imageFile.name.endsWith(".webp", true) -> "image/webp"
                                        imageFile.name.endsWith(".gif", true) -> "image/gif"
                                        else -> "image/jpeg"
                                    }
                                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        setDataAndType(uri, mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, context.getString(R.string.set_as_wallpaper))
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.wallpaper_set_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                )
            }
        }
    }
}

/**
 * 准备可供 FileProvider 共享的图片文件（带正确的图片扩展名）。
 * WallpaperManager.getCropAndSetWallpaperIntent 会通过 ContentResolver.getType()
 * 检查 MIME 类型，FileProvider 根据文件扩展名推断 MIME，所以必须保证扩展名正确。
 */
private suspend fun prepareShareableImageFile(
    context: android.content.Context,
    imageUrl: String
): File = withContext(Dispatchers.IO) {
    val shareDir = File(context.cacheDir, "wallpaper_share").apply { mkdirs() }
    // 清理旧文件
    shareDir.listFiles()?.forEach { it.delete() }

    // 从 URL 推断扩展名，回退到 jpg
    val extension = imageUrl.substringAfterLast('.', "jpg")
        .substringBefore('?')
        .substringBefore('/')
        .lowercase()
        .let { ext -> if (ext in listOf("jpg", "jpeg", "png", "webp", "gif")) ext else "jpg" }

    val destFile = File(shareDir, "wallpaper.$extension")

    // 优先使用 LoadTaskManager 的缓存
    val cached = LoadTaskManager.getCachedFilePath(imageUrl)
    if (cached != null) {
        File(cached).copyTo(destFile, overwrite = true)
        return@withContext destFile
    }

    // 没有缓存，下载到目标文件
    val request = Request.Builder().url(imageUrl).build()
    PixivClient.imageClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        val body = response.body ?: throw Exception("Empty body")
        destFile.sink().buffer().use { sink ->
            body.source().use { source ->
                sink.writeAll(source)
            }
        }
    }
    destFile
}
