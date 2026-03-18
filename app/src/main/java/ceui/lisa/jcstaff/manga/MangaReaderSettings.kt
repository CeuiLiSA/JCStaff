package ceui.lisa.jcstaff.manga

/** Which direction pages advance: LTR = western comics, RTL = Japanese manga */
enum class ReadingDirection { LTR, RTL }

/** How pages are presented */
enum class ReadingMode { PAGED, SCROLL }

/** How many pages are shown at once in PAGED mode */
enum class PageDisplayMode { SINGLE, DOUBLE }

data class MangaReaderSettings(
    val readingDirection: ReadingDirection = ReadingDirection.RTL,
    val readingMode: ReadingMode = ReadingMode.PAGED,
    val pageDisplayMode: PageDisplayMode = PageDisplayMode.SINGLE,
    val autoHideControls: Boolean = true,
    val keepScreenOn: Boolean = true,
)

data class MangaPage(
    val index: Int,
    val previewUrl: String,   // large image, fast to load
    val originalUrl: String?, // full-resolution
)
