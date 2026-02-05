package ceui.lisa.jcstaff.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.TrendingTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索建议状态
 */
data class SuggestionsState(
    val suggestions: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 热门标签状态
 */
data class TrendingTagsState(
    val tags: List<TrendingTag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 搜索历史状态
 */
data class SearchHistoryState(
    val history: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 搜索页面 ViewModel
 *
 * 管理搜索历史、关键词联想和热门标签
 */
@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {

    private val _searchHistoryState = MutableStateFlow(SearchHistoryState())
    val searchHistoryState: StateFlow<SearchHistoryState> = _searchHistoryState.asStateFlow()

    private val _suggestionsState = MutableStateFlow(SuggestionsState())
    val suggestionsState: StateFlow<SuggestionsState> = _suggestionsState.asStateFlow()

    private val _trendingTagsState = MutableStateFlow(TrendingTagsState())
    val trendingTagsState: StateFlow<TrendingTagsState> = _trendingTagsState.asStateFlow()

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText.asStateFlow()

    private var autocompleteJob: Job? = null

    init {
        observeSearchHistory()
        loadTrendingTags()
        observeQueryForAutocomplete()
    }

    /**
     * 观察搜索历史
     */
    private fun observeSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            BrowseHistoryRepository.getSearchHistoryFlow()
                .catch { e ->
                    _searchHistoryState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载搜索历史失败")
                    }
                }
                .collect { history ->
                    _searchHistoryState.update {
                        it.copy(history = history, isLoading = false, error = null)
                    }
                }
        }
    }

    /**
     * 加载热门标签
     */
    private fun loadTrendingTags() {
        viewModelScope.launch(Dispatchers.IO) {
            _trendingTagsState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = PixivClient.pixivApi.getTrendingTags()
                _trendingTagsState.update {
                    it.copy(tags = response.trend_tags, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _trendingTagsState.update {
                    it.copy(isLoading = false, error = e.message ?: "加载热门标签失败")
                }
            }
        }
    }

    /**
     * 观察查询文本变化，进行 debounce 联想
     */
    private fun observeQueryForAutocomplete() {
        autocompleteJob = viewModelScope.launch(Dispatchers.IO) {
            _queryText
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collectLatest { word ->
                    _suggestionsState.update { it.copy(isLoading = true, error = null) }
                    try {
                        val response = PixivClient.pixivApi.searchAutocomplete(word.trim())
                        _suggestionsState.update {
                            it.copy(suggestions = response.tags, isLoading = false, error = null)
                        }
                    } catch (e: Exception) {
                        _suggestionsState.update {
                            it.copy(suggestions = emptyList(), isLoading = false, error = e.message)
                        }
                    }
                }
        }
    }

    /**
     * 更新查询文本
     */
    fun updateQuery(query: String) {
        _queryText.value = query
        // 清空输入时清空联想
        if (query.isBlank()) {
            _suggestionsState.update { it.copy(suggestions = emptyList(), isLoading = false) }
        }
    }

    /**
     * 记录搜索历史
     */
    fun recordSearch(tag: Tag) {
        if (!tag.name.isNullOrBlank()) {
            BrowseHistoryRepository.recordSearch(tag)
        }
    }

    /**
     * 删除指定的搜索历史
     */
    fun deleteSearchTag(tagName: String) {
        BrowseHistoryRepository.deleteSearchTag(tagName)
    }

    /**
     * 刷新热门标签
     */
    fun refreshTrendingTags() {
        loadTrendingTags()
    }

    /**
     * 清空搜索建议
     */
    fun clearSuggestions() {
        _suggestionsState.update { it.copy(suggestions = emptyList(), isLoading = false) }
    }
}