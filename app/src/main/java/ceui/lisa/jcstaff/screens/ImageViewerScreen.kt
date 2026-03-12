package ceui.lisa.jcstaff.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.downloadToGallery
import ceui.lisa.jcstaff.core.saveFromCacheToGallery
import ceui.lisa.jcstaff.components.animations.LocalSharedTransitionScope
import ceui.lisa.jcstaff.components.animations.LocalAnimatedVisibilityScope
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.network.PixivClient
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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

    // Shared element transition scopes
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val onLongPress: () -> Unit = { showBottomSheet = true }

    // 入场动画：0→1，背景渐入 + 图片从底部滑入
    val enterProgress = remember { Animatable(0f) }
    val isEntering = enterProgress.value < 1f
    LaunchedEffect(Unit) {
        enterProgress.animateTo(
            1f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    }

    // Drag-to-dismiss — 小红书风格：大幅缩放 + 背景渐隐 + 飞出动画
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // 按压点像素坐标（容器内），用于补偿缩放偏移
    var pressPxX by remember { mutableFloatStateOf(0f) }
    var pressPxY by remember { mutableFloatStateOf(0f) }
    val springBackX = remember { Animatable(0f) }
    val springBackY = remember { Animatable(0f) }
    // dismiss 飞出动画进度 0→1
    val dismissAnimProgress = remember { Animatable(0f) }
    var dismissStartX by remember { mutableFloatStateOf(0f) }
    var dismissStartY by remember { mutableFloatStateOf(0f) }
    var dismissStartScale by remember { mutableFloatStateOf(1f) }
    val isDismissing = dismissAnimProgress.isRunning

    val dismissThreshold = 400f
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // 飞出目标：屏幕底部再远一点，缩到 0
    val dismissTargetY = screenHeightPx * 0.8f
    val dismissTargetScale = 0f

    val effectiveOffsetX: Float
    val effectiveOffsetY: Float
    val contentScale: Float
    val bgAlpha: Float

    if (isDismissing) {
        val t = dismissAnimProgress.value
        effectiveOffsetX = dismissStartX
        effectiveOffsetY = dismissStartY + (dismissTargetY - dismissStartY) * t
        contentScale = dismissStartScale + (dismissTargetScale - dismissStartScale) * t
        bgAlpha = ((1f - t) * (1f - (kotlin.math.abs(dismissStartY) / (screenHeightPx * 0.5f)).coerceIn(0f, 1f))).coerceIn(0f, 1f)
    } else if (isEntering) {
        // 入场：只渐入背景，图片保持原位不动（底下详情页的图片提供视觉连续性）
        effectiveOffsetX = 0f
        effectiveOffsetY = 0f
        contentScale = 1f
        bgAlpha = enterProgress.value
    } else {
        effectiveOffsetX = if (springBackX.isRunning) springBackX.value else dragOffsetX
        effectiveOffsetY = if (springBackY.isRunning) springBackY.value else dragOffsetY
        val dragProgress = (kotlin.math.abs(effectiveOffsetY) / (screenHeightPx * 0.5f)).coerceIn(0f, 1f)
        bgAlpha = (1f - dragProgress).coerceIn(0f, 1f)
        contentScale = (1f - dragProgress * 0.5f).coerceIn(0.5f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
    ) {
        // 图片容器 — 缩放中心在容器中心，通过补偿偏移让按压点跟手
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 在 graphicsLayer 内部用实际 layer 尺寸计算补偿
                    val layerCenterX = size.width / 2f
                    val layerCenterY = size.height / 2f
                    val compX = (pressPxX - layerCenterX) * (1f - contentScale)
                    val compY = (pressPxY - layerCenterY) * (1f - contentScale)
                    translationX = effectiveOffsetX + compX
                    translationY = effectiveOffsetY + compY
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // 记录按压点像素坐标
                            pressPxX = offset.x
                            pressPxY = offset.y
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (kotlin.math.abs(dragOffsetY) > dismissThreshold) {
                                    // 记录 dismiss 起始状态
                                    val curProgress = (kotlin.math.abs(dragOffsetY) / (screenHeightPx * 0.5f)).coerceIn(0f, 1f)
                                    dismissStartX = dragOffsetX
                                    dismissStartY = dragOffsetY
                                    dismissStartScale = (1f - curProgress * 0.5f).coerceIn(0.5f, 1f)
                                    // 清除拖拽状态，由 dismissAnimProgress 接管
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    // 播放飞出动画
                                    dismissAnimProgress.snapTo(0f)
                                    dismissAnimProgress.animateTo(
                                        1f,
                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                    )
                                    navViewModel.goBack()
                                } else if (dragOffsetX != 0f || dragOffsetY != 0f) {
                                    springBackX.snapTo(dragOffsetX)
                                    springBackY.snapTo(dragOffsetY)
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    val easeBack = tween<Float>(250, easing = FastOutSlowInEasing)
                                    launch { springBackX.animateTo(0f, animationSpec = easeBack) }
                                    launch { springBackY.animateTo(0f, animationSpec = easeBack) }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                if (dragOffsetX != 0f || dragOffsetY != 0f) {
                                    springBackX.snapTo(dragOffsetX)
                                    springBackY.snapTo(dragOffsetY)
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    launch { springBackX.animateTo(0f) }
                                    launch { springBackY.animateTo(0f) }
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount.x
                            dragOffsetY += dragAmount.y
                        }
                    )
                }
        ) {
            // 预览图（作为底层，在原图加载完成前显示）
            @OptIn(ExperimentalSharedTransitionApi::class)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState("illust_image_$sharedElementKey"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else Modifier
                    )
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
