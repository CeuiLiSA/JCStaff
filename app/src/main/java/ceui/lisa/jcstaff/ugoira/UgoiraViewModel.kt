package ceui.lisa.jcstaff.ugoira

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.saveFileToGallery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ugoira ViewModel
 */
class UgoiraViewModel : ViewModel() {

    private val _state = MutableStateFlow<UgoiraState>(UgoiraState.Idle)
    val state: StateFlow<UgoiraState> = _state.asStateFlow()

    // Separate state flow used during export operations to avoid disrupting the player
    private val _exportState = MutableStateFlow<UgoiraState>(UgoiraState.Idle)
    val exportState: StateFlow<UgoiraState> = _exportState.asStateFlow()

    private var currentIllustId: Long? = null

    fun load(context: Context, illustId: Long) {
        if (currentIllustId == illustId && _state.value is UgoiraState.Done) {
            return
        }

        currentIllustId = illustId
        viewModelScope.launch {
            UgoiraRepository.getOrCreateGif(context, illustId, _state)
        }
    }

    fun retry(context: Context) {
        currentIllustId?.let { id ->
            _state.value = UgoiraState.Idle
            load(context, id)
        }
    }

    fun getData(): UgoiraData? {
        return (_state.value as? UgoiraState.Done)?.data
    }

    /**
     * Save to gallery in the specified format.
     * GIF reuses the already-created file; WebP and MP4 are encoded on demand.
     */
    suspend fun saveToGallery(
        context: Context,
        format: UgoiraExportFormat = UgoiraExportFormat.GIF
    ): Result<Unit> {
        val illustId = currentIllustId
            ?: return Result.failure(UgoiraException(R.string.ugoira_error_not_generated))

        // If GIF is already done and that's the requested format, save immediately
        if (format == UgoiraExportFormat.GIF) {
            val gifFile = UgoiraRepository.getGifFile(illustId)
                ?: return Result.failure(UgoiraException(R.string.ugoira_error_not_generated))
            return saveFileToGallery(
                context = context,
                sourceFile = gifFile,
                fileName = "pixiv_$illustId.${format.extension}",
                mimeType = format.mimeType
            )
        }

        // For WebP / MP4: encode then save (may re-use cached frames)
        _exportState.value = UgoiraState.Idle
        val data = UgoiraRepository.getOrCreate(context, illustId, format, _exportState)
            ?: return Result.failure(UgoiraException(R.string.ugoira_error_processing))

        return saveFileToGallery(
            context = context,
            sourceFile = data.gifFile,
            fileName = "pixiv_$illustId.${format.extension}",
            mimeType = format.mimeType
        )
    }
}
