package ceui.lisa.jcstaff.ugoira

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /**
     * 加载 ugoira
     */
    fun load(context: Context, illustId: Long) {
        if (currentIllustId == illustId && _state.value is UgoiraState.Done) {
            return // 已加载
        }

        currentIllustId = illustId
        viewModelScope.launch {
            UgoiraRepository.getOrCreateFrames(context, illustId, _state)
        }
    }

    /**
     * 重试
     */
    fun retry(context: Context) {
        currentIllustId?.let { id ->
            _state.value = UgoiraState.Idle
            load(context, id)
        }
    }

    /**
     * 获取当前帧数据
     */
    fun getFrames(): UgoiraFrames? {
        return (_state.value as? UgoiraState.Done)?.frames
    }

    /**
     * 获取第一帧文件（用于保存）
     */
    fun getFirstFrameFile(): java.io.File? {
        return currentIllustId?.let { UgoiraRepository.getFirstFrameFile(it) }
    }
}
