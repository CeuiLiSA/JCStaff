package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 推荐插画 ViewModel（旧版兼容）
 * 内部委托给 RecommendedContentViewModel("illust")
 */
class RecommendedViewModel : ViewModel() {

    private val delegate = RecommendedContentViewModel("illust")

    val state: StateFlow<RecommendedUiState> = delegate.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { delegate.load() }
    }

    fun loadMore() {
        viewModelScope.launch { delegate.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { delegate.refresh() }
    }
}
