package ceui.lisa.jcstaff.core

import android.net.Uri
import androidx.core.content.FileProvider

class ImageFileProvider : FileProvider() {
    override fun getType(uri: Uri): String {
        val fileName = uri.lastPathSegment ?: return "image/jpeg"
        return when {
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
