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
import kotlinx.coroutines.launch

/**
 * 插画浏览历史状态
 */
data class IllustHistoryState(
    val illusts: List<Illust> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val totalCount: Int = 0,
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
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val totalCount: Int = 0,
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
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val totalCount: Int = 0,
    val error: String? = null
) {
    val isEmpty: Boolean get() = users.isEmpty() && !isLoading
}

/**
 * 浏览历史 ViewModel
 *
 * 支持分页加载，每页30条记录
 */
class BrowseHistoryViewModel : ViewModel() {

    private val _illustState = MutableStateFlow(IllustHistoryState())
    val illustState: StateFlow<IllustHistoryState> = _illustState.asStateFlow()

    private val _novelState = MutableStateFlow(NovelHistoryState())
    val novelState: StateFlow<NovelHistoryState> = _novelState.asStateFlow()

    private val _userState = MutableStateFlow(UserHistoryState())
    val userState: StateFlow<UserHistoryState> = _userState.asStateFlow()

    private var illustPage = 0
    private var novelPage = 0
    private var userPage = 0

    init {
        loadIllustHistory()
        loadNovelHistory()
        loadUserHistory()
    }

    private fun loadIllustHistory() {
        viewModelScope.launch {
            runCatching {
                val totalCount = BrowseHistoryRepository.getIllustHistoryCount()
                val illusts = BrowseHistoryRepository.getIllustHistoryPage(0)
                _illustState.value = IllustHistoryState(
                    illusts = illusts,
                    isLoading = false,
                    canLoadMore = illusts.size < totalCount,
                    totalCount = totalCount
                )
                illustPage = 1
            }.onFailure { e ->
                _illustState.value = _illustState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载历史失败"
                )
            }
        }
    }

    fun loadMoreIllusts() {
        val state = _illustState.value
        if (state.isLoadingMore || !state.canLoadMore) return

        _illustState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                val newIllusts = BrowseHistoryRepository.getIllustHistoryPage(illustPage)
                val allIllusts = state.illusts + newIllusts
                _illustState.value = state.copy(
                    illusts = allIllusts,
                    isLoadingMore = false,
                    canLoadMore = allIllusts.size < state.totalCount
                )
                illustPage++
            }.onFailure {
                _illustState.value = state.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadNovelHistory() {
        viewModelScope.launch {
            runCatching {
                val totalCount = BrowseHistoryRepository.getNovelHistoryCount()
                val novels = BrowseHistoryRepository.getNovelHistoryPage(0)
                _novelState.value = NovelHistoryState(
                    novels = novels,
                    isLoading = false,
                    canLoadMore = novels.size < totalCount,
                    totalCount = totalCount
                )
                novelPage = 1
            }.onFailure { e ->
                _novelState.value = _novelState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载历史失败"
                )
            }
        }
    }

    fun loadMoreNovels() {
        val state = _novelState.value
        if (state.isLoadingMore || !state.canLoadMore) return

        _novelState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                val newNovels = BrowseHistoryRepository.getNovelHistoryPage(novelPage)
                val allNovels = state.novels + newNovels
                _novelState.value = state.copy(
                    novels = allNovels,
                    isLoadingMore = false,
                    canLoadMore = allNovels.size < state.totalCount
                )
                novelPage++
            }.onFailure {
                _novelState.value = state.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadUserHistory() {
        viewModelScope.launch {
            runCatching {
                val totalCount = BrowseHistoryRepository.getUserHistoryCount()
                val users = BrowseHistoryRepository.getUserHistoryPage(0)
                _userState.value = UserHistoryState(
                    users = users,
                    isLoading = false,
                    canLoadMore = users.size < totalCount,
                    totalCount = totalCount
                )
                userPage = 1
            }.onFailure { e ->
                _userState.value = _userState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载历史失败"
                )
            }
        }
    }

    fun loadMoreUsers() {
        val state = _userState.value
        if (state.isLoadingMore || !state.canLoadMore) return

        _userState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                val newUsers = BrowseHistoryRepository.getUserHistoryPage(userPage)
                val allUsers = state.users + newUsers
                _userState.value = state.copy(
                    users = allUsers,
                    isLoadingMore = false,
                    canLoadMore = allUsers.size < state.totalCount
                )
                userPage++
            }.onFailure {
                _userState.value = state.copy(isLoadingMore = false)
            }
        }
    }

    fun clearIllustHistory() {
        BrowseHistoryRepository.clearIllustHistory()
        _illustState.value = IllustHistoryState(isLoading = false, canLoadMore = false, totalCount = 0)
        illustPage = 0
    }

    fun clearNovelHistory() {
        BrowseHistoryRepository.clearNovelHistory()
        _novelState.value = NovelHistoryState(isLoading = false, canLoadMore = false, totalCount = 0)
        novelPage = 0
    }

    fun clearUserHistory() {
        BrowseHistoryRepository.clearUserHistory()
        _userState.value = UserHistoryState(isLoading = false, canLoadMore = false, totalCount = 0)
        userPage = 0
    }

    fun deleteIllust(illustId: Long) {
        BrowseHistoryRepository.deleteIllust(illustId)
        _illustState.value = _illustState.value.copy(
            illusts = _illustState.value.illusts.filter { it.id != illustId },
            totalCount = _illustState.value.totalCount - 1
        )
    }

    fun deleteNovel(novelId: Long) {
        BrowseHistoryRepository.deleteNovel(novelId)
        _novelState.value = _novelState.value.copy(
            novels = _novelState.value.novels.filter { it.id != novelId },
            totalCount = _novelState.value.totalCount - 1
        )
    }

    fun deleteUser(userId: Long) {
        BrowseHistoryRepository.deleteUser(userId)
        _userState.value = _userState.value.copy(
            users = _userState.value.users.filter { it.id != userId },
            totalCount = _userState.value.totalCount - 1
        )
    }

    fun clearAll() {
        clearIllustHistory()
        clearNovelHistory()
        clearUserHistory()
    }
}
