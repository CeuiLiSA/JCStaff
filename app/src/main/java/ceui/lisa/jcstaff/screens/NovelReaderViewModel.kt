package ceui.lisa.jcstaff.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NovelReaderState(
    val novelText: String? = null,
    val seriesPrev: Novel? = null,
    val seriesNext: Novel? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val fontSize: Int = 16,
    val lineHeightMultiplier: Float = 1.8f
)

class NovelReaderViewModel(private val novelId: Long) : ViewModel() {

    private val _state = MutableStateFlow(NovelReaderState())
    val state: StateFlow<NovelReaderState> = _state.asStateFlow()

    init {
        loadNovelText()
    }

    fun loadNovelText() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, hasError = false) }
            try {
                val response = PixivClient.getNovelText(novelId)
                response.series_prev?.let { ObjectStore.put(it) }
                response.series_next?.let { ObjectStore.put(it) }
                _state.update {
                    it.copy(
                        novelText = response.novel_text,
                        seriesPrev = response.series_prev,
                        seriesNext = response.series_next,
                        isLoading = false,
                        hasError = false
                    )
                }
            } catch (e: Exception) {
                Log.e("NovelReaderVM", "loadNovelText failed for novelId=$novelId", e)
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun setFontSize(size: Int) {
        _state.update { it.copy(fontSize = size.coerceIn(12, 28)) }
    }

    fun setLineHeight(multiplier: Float) {
        _state.update { it.copy(lineHeightMultiplier = multiplier.coerceIn(1.2f, 3.0f)) }
    }

    companion object {
        fun factory(novelId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NovelReaderViewModel(novelId) as T
            }
        }
    }
}
