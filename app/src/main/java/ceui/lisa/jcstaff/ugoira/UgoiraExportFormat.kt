package ceui.lisa.jcstaff.ugoira

enum class UgoiraExportFormat(val extension: String, val mimeType: String) {
    GIF("gif", "image/gif"),
    WEBP("webp", "image/webp"),
    MP4("mp4", "video/mp4")
}
