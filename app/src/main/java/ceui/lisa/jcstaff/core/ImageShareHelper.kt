package ceui.lisa.jcstaff.core

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

/**
 * 准备可供 FileProvider 共享的图片文件（带正确扩展名）。
 * 优先使用 LoadTaskManager 缓存，否则从网络下载。
 */
suspend fun prepareShareableImageFile(
    context: Context,
    imageUrl: String,
    fileName: String = "pixiv"
): File = withContext(Dispatchers.IO) {
    val shareDir = File(context.cacheDir, "image_share").apply { mkdirs() }
    shareDir.listFiles()?.forEach { it.delete() }

    val extension = imageUrl.substringAfterLast('.', "jpg")
        .substringBefore('?')
        .substringBefore('/')
        .lowercase()
        .let { ext -> if (ext in listOf("jpg", "jpeg", "png", "webp", "gif")) ext else "jpg" }

    val destFile = File(shareDir, "$fileName.$extension")

    val cached = LoadTaskManager.getCachedFilePath(imageUrl)
    if (cached != null) {
        File(cached).copyTo(destFile, overwrite = true)
        return@withContext destFile
    }

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

/**
 * 分享图片文件到其他 App
 */
fun shareImageFile(context: Context, imageFile: File) {
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
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_image))
    )
}
