package ceui.lisa.jcstaff.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 插画浏览历史状态
 */
data class IllustHistoryState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isEmpty: Boolean get() = illusts.isEmpty() && !isLoading
}

/**
 * 小说浏览历史状态
 */
data class NovelHistoryState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isEmpty: Boolean get() = novels.isEmpty() && !isLoading
}

/**
 * 用户浏览历史状态
 */
data class UserHistoryState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isEmpty: Boolean get() = users.isEmpty() && !isLoading
}

/**
 * 浏览历史 ViewModel
 *
 * 观察 Room Flow，自动响应数据变化，支持插画、小说、用户三种历史
 */
class BrowseHistoryViewModel : ViewModel() {

    private val _illustState = MutableStateFlow(IllustHistoryState())
    val illustState: StateFlow<IllustHistoryState> = _illustState.asStateFlow()

    private val _novelState = MutableStateFlow(NovelHistoryState())
    val novelState: StateFlow<NovelHistoryState> = _novelState.asStateFlow()

    private val _userState = MutableStateFlow(UserHistoryState())
    val userState: StateFlow<UserHistoryState> = _userState.asStateFlow()

    init {
        observeIllustHistory()
        observeNovelHistory()
        observeUserHistory()
    }

    private fun observeIllustHistory() {
        viewModelScope.launch {
            BrowseHistoryRepository.getIllustHistoryFlow()
                .catch { e ->
                    _illustState.value = _illustState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载历史失败"
                    )
                }
                .collect { illusts ->
                    _illustState.value = _illustState.value.copy(
                        illusts = illusts,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    private fun observeNovelHistory() {
        viewModelScope.launch {
            BrowseHistoryRepository.getNovelHistoryFlow()
                .catch { e ->
                    _novelState.value = _novelState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载历史失败"
                    )
                }
                .collect { novels ->
                    _novelState.value = _novelState.value.copy(
                        novels = novels,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    private fun observeUserHistory() {
        viewModelScope.launch {
            BrowseHistoryRepository.getUserHistoryFlow()
                .catch { e ->
                    _userState.value = _userState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载历史失败"
                    )
                }
                .collect { users ->
                    _userState.value = _userState.value.copy(
                        users = users,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun clearIllustHistory() {
        BrowseHistoryRepository.clearIllustHistory()
    }

    fun clearNovelHistory() {
        BrowseHistoryRepository.clearNovelHistory()
    }

    fun clearUserHistory() {
        BrowseHistoryRepository.clearUserHistory()
    }

    fun deleteIllust(illustId: Long) {
        BrowseHistoryRepository.deleteIllust(illustId)
    }

    fun deleteNovel(novelId: Long) {
        BrowseHistoryRepository.deleteNovel(novelId)
    }

    fun deleteUser(userId: Long) {
        BrowseHistoryRepository.deleteUser(userId)
    }

    fun clearAll() {
        BrowseHistoryRepository.clearIllustHistory()
        BrowseHistoryRepository.clearNovelHistory()
        BrowseHistoryRepository.clearUserHistory()
    }
}
