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

    suspend fun saveToGallery(context: Context): Result<Unit> {
        val illustId = currentIllustId
            ?: return Result.failure(UgoiraException(R.string.ugoira_error_not_generated))

        val gifFile = UgoiraRepository.getGifFile(illustId)
            ?: return Result.failure(UgoiraException(R.string.ugoira_error_not_generated))

        return saveFileToGallery(
            context = context,
            sourceFile = gifFile,
            fileName = "pixiv_${illustId}.gif",
            mimeType = "image/gif"
        )
    }
}
