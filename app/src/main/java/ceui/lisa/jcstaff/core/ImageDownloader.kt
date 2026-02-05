package ceui.lisa.jcstaff.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 批量下载进度回调
 */
data class BatchDownloadProgress(
    val current: Int,
    val total: Int,
    val currentIllustId: Long,
    val isSuccess: Boolean
)

/**
 * 下载图片并保存到相册
 * @param context Context
 * @param imageUrl 图片URL
 * @param fileName 文件名（不含扩展名）
 * @return 是否成功
 */
suspend fun downloadToGallery(
    context: Context,
    imageUrl: String,
    fileName: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        // 构建请求（Referer 由 PixivClient.imageClient 的拦截器添加）
        val request = Request.Builder()
            .url(imageUrl)
            .addHeader("User-Agent", "PixivAndroidApp/5.0.234 (Android 11; Pixel 5)")
            .build()

        // 执行请求
        val response = PixivClient.imageClient.newCall(request).execute()

        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("HTTP ${response.code}"))
        }

        val inputStream = response.body?.byteStream()
            ?: return@withContext Result.failure(Exception("Empty response body"))

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        response.close()

        if (bitmap == null) {
            return@withContext Result.failure(Exception("Failed to decode image"))
        }

        // 保存到相册
        saveToGallery(context, bitmap, fileName)
        bitmap.recycle()

        Result.success(Unit)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

private fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String) {
    val format = if (fileName.endsWith(".png", ignoreCase = true)) {
        Bitmap.CompressFormat.PNG
    } else {
        Bitmap.CompressFormat.JPEG
    }

    val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
    val extension = if (format == Bitmap.CompressFormat.PNG) ".png" else ".jpg"
    val finalFileName = if (fileName.contains(".")) fileName else "$fileName$extension"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ 使用 MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/JCStaff")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(format, 100, outputStream)
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    } else {
        // Android 9 及以下
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "JCStaff")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val file = File(appDir, finalFileName)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(format, 100, outputStream)
        }

        // 通知媒体库扫描
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
            null
        )
    }
}

/**
 * 从缓存文件保存到相册（瞬间完成）
 * @param context Context
 * @param cachedFilePath 缓存文件路径
 * @param fileName 文件名（不含扩展名）
 * @return 是否成功
 */
suspend fun saveFromCacheToGallery(
    context: Context,
    cachedFilePath: String,
    fileName: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val file = File(cachedFilePath)
        if (!file.exists()) {
            return@withContext Result.failure(Exception("Cache file not found"))
        }

        val bitmap = BitmapFactory.decodeFile(cachedFilePath)
            ?: return@withContext Result.failure(Exception("Failed to decode cached image"))

        saveToGallery(context, bitmap, fileName)
        bitmap.recycle()

        Result.success(Unit)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

/**
 * 直接保存文件到相册（支持任意格式，包括动态 WebP）
 * @param context Context
 * @param sourceFile 源文件
 * @param fileName 文件名（含扩展名）
 * @param mimeType MIME 类型
 * @return 是否成功
 */
suspend fun saveFileToGallery(
    context: Context,
    sourceFile: File,
    fileName: String,
    mimeType: String = "image/webp"
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        if (!sourceFile.exists()) {
            return@withContext Result.failure(Exception("Source file not found"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/JCStaff"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "JCStaff")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val destFile = File(appDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        }

        Result.success(Unit)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

/**
 * 批量下载图片到相册（立即执行，不进入队列）
 * @param context Context
 * @param illusts 要下载的 Illust 列表
 * @param onProgress 进度回调
 * @return 成功下载的数量
 */
suspend fun batchDownloadToGallery(
    context: Context,
    illusts: List<Illust>,
    onProgress: (BatchDownloadProgress) -> Unit = {}
): Int = withContext(Dispatchers.IO) {
    var successCount = 0
    val total = illusts.size

    illusts.forEachIndexed { index, illust ->
        val imageUrl = illust.maxUrl() ?: illust.previewUrl()
        val fileName = "pixiv_${illust.id}"

        val result = downloadToGallery(
            context = context,
            imageUrl = imageUrl,
            fileName = fileName
        )

        val isSuccess = result.isSuccess
        if (isSuccess) {
            successCount++
        }

        onProgress(
            BatchDownloadProgress(
                current = index + 1,
                total = total,
                currentIllustId = illust.id,
                isSuccess = isSuccess
            )
        )
    }

    successCount
}
