package ceui.lisa.jcstaff.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object ImageDownloader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 下载图片并保存到相册
     * @param context Context
     * @param imageUrl 图片URL
     * @param fileName 文件名（不含扩展名）
     * @param referer Referer header（Pixiv 需要）
     * @return 是否成功
     */
    suspend fun downloadToGallery(
        context: Context,
        imageUrl: String,
        fileName: String,
        referer: String = "https://app-api.pixiv.net/"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 构建请求
            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("Referer", referer)
                .addHeader("User-Agent", "PixivAndroidApp/5.0.234 (Android 11; Pixel 5)")
                .build()

            // 执行请求
            val response = client.newCall(request).execute()

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
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
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
}
