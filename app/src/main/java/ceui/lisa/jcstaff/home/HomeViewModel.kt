package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recommendedIllusts: List<Illust> = emptyList(),
    val followingIllusts: List<Illust> = emptyList(),
    val isLoadingRecommended: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val recommendedError: String? = null,
    val followingError: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecommendedIllusts()
        loadFollowingIllusts()
    }

    fun loadRecommendedIllusts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingRecommended = true,
                recommendedError = null
            )
            try {
                val response = PixivClient.pixivApi.getRecommendedIllusts()
                val illusts = response.displayList

                // 存入 ObjectStore
                storeIllusts(illusts)

                _uiState.value = _uiState.value.copy(
                    recommendedIllusts = illusts,
                    isLoadingRecommended = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingRecommended = false,
                    recommendedError = e.message ?: "加载失败"
                )
            }
        }
    }

    fun loadFollowingIllusts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingFollowing = true,
                followingError = null
            )
            try {
                val response = PixivClient.pixivApi.getFollowingIllusts()
                val illusts = response.illusts

                // 存入 ObjectStore
                storeIllusts(illusts)

                _uiState.value = _uiState.value.copy(
                    followingIllusts = illusts,
                    isLoadingFollowing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingFollowing = false,
                    followingError = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 将插画列表存入 ObjectStore
     * 同时存储关联的 User
     */
    private fun storeIllusts(illusts: List<Illust>) {
        illusts.forEach { illust ->
            ObjectStore.put(illust)
            illust.user?.let { user ->
                ObjectStore.put(user)
            }
        }
    }

    fun refresh() {
        loadRecommendedIllusts()
        loadFollowingIllusts()
    }
}
