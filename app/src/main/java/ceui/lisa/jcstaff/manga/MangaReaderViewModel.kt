package ceui.lisa.jcstaff.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MangaReaderViewModel(val illustId: Long) : ViewModel() {

    // ── Page data ──────────────────────────────────────────────────────────

    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _illustTitle = MutableStateFlow("")
    val illustTitle: StateFlow<String> = _illustTitle.asStateFlow()

    // ── Reading position ───────────────────────────────────────────────────

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // ── UI visibility ──────────────────────────────────────────────────────

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private var autoHideJob: Job? = null

    // ── Settings ───────────────────────────────────────────────────────────

    private val _settings = MutableStateFlow(MangaReaderSettings())
    val settings: StateFlow<MangaReaderSettings> = _settings.asStateFlow()

    // ── Init ───────────────────────────────────────────────────────────────

    init {
        loadFromApi()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    fun loadFromIllust(illust: Illust) {
        _illustTitle.value = illust.title ?: ""
        val pageList = buildPageList(illust)
        _pages.value = pageList
        _isLoading.value = false
    }

    fun loadFromApi() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = PixivClient.pixivApi.getIllustDetail(illustId)
                val illust = response.illust ?: run {
                    _error.value = "作品不存在"
                    return@launch
                }
                loadFromIllust(illust)
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun goToPage(page: Int) {
        val max = (_pages.value.size - 1).coerceAtLeast(0)
        _currentPage.value = page.coerceIn(0, max)
        scheduleAutoHide()
    }

    fun prevPage() = goToPage(_currentPage.value - 1)
    fun nextPage() = goToPage(_currentPage.value + 1)

    fun showControls() {
        _showControls.value = true
        scheduleAutoHide()
    }

    fun toggleControls() {
        _showControls.value = !_showControls.value
        if (_showControls.value) scheduleAutoHide()
        else autoHideJob?.cancel()
    }

    fun keepControlsVisible() {
        autoHideJob?.cancel()
    }

    fun updateSettings(newSettings: MangaReaderSettings) {
        _settings.value = newSettings
        if (newSettings.autoHideControls && _showControls.value) {
            scheduleAutoHide()
        } else {
            autoHideJob?.cancel()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun buildPageList(illust: Illust): List<MangaPage> {
        return if (illust.page_count <= 1) {
            listOf(
                MangaPage(
                    index = 0,
                    previewUrl = illust.previewUrl(),
                    originalUrl = illust.meta_single_page?.original_image_url
                )
            )
        } else {
            illust.meta_pages?.mapIndexed { i, page ->
                MangaPage(
                    index = i,
                    previewUrl = page.image_urls?.large ?: page.image_urls?.medium ?: "",
                    originalUrl = page.image_urls?.original
                )
            } ?: emptyList()
        }
    }

    private fun scheduleAutoHide() {
        if (!_settings.value.autoHideControls) return
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(3_000)
            _showControls.value = false
        }
    }

    companion object {
        fun factory(illustId: Long) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MangaReaderViewModel(illustId) as T
            }
        }
    }
}
