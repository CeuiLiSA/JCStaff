package ceui.lisa.jcstaff.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.PagedResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 通用分页 ViewModel 基类
 *
 * 封装 PagedDataLoader 的标准模式：构造 loader、暴露 state、提供 loadMore/refresh。
 * 子类只需传入 loader 构造参数即可。
 */
abstract class PagedViewModel<T, R : PagedResponse<T>>(
    cacheConfig: CacheConfig? = null,
    responseClass: Class<R>,
    loadFirstPage: suspend () -> R,
    onItemsLoaded: (List<T>) -> Unit = {}
) : ViewModel() {

    protected val loader = PagedDataLoader(
        cacheConfig = cacheConfig,
        responseClass = responseClass,
        loadFirstPage = loadFirstPage,
        onItemsLoaded = onItemsLoaded
    )

    val state: StateFlow<PagedState<T>> = loader.state

    init {
        viewModelScope.launch { loader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
    }
}
