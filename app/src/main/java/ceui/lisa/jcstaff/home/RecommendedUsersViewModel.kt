package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.network.UserPreviewResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendedUsersUiState(
    val users: List<UserPreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null
) {
    val canLoadMore: Boolean get() = nextUrl != null && !isLoadingMore
}

class RecommendedUsersViewModel : ViewModel() {

    private val _state = MutableStateFlow(RecommendedUsersUiState())
    val state: StateFlow<RecommendedUsersUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            val cachedResponse = PixivClient.getFromStaleCache(
                path = "/v1/user/recommended",
                queryParams = mapOf("filter" to "for_ios"),
                clazz = UserPreviewResponse::class.java
            )

            if (cachedResponse != null) {
                storeUserPreviews(cachedResponse.user_previews)
                _state.value = _state.value.copy(
                    users = cachedResponse.user_previews,
                    isLoading = false,
                    nextUrl = cachedResponse.next_url
                )
            }

            try {
                val response = PixivClient.pixivApi.getRecommendedUsers()
                storeUserPreviews(response.user_previews)
                _state.value = _state.value.copy(
                    users = response.user_previews,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.users.isEmpty()) {
                        e.message ?: "加载失败"
                    } else null
                )
            }
        }
    }

    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            try {
                val response = PixivClient.pixivApi.getNextPageUserPreviews(nextUrl)
                storeUserPreviews(response.user_previews)
                _state.value = _state.value.copy(
                    users = _state.value.users + response.user_previews,
                    isLoadingMore = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }

    fun refresh() {
        load()
    }

    private fun storeUserPreviews(previews: List<UserPreview>) {
        previews.forEach { preview ->
            preview.user?.let { user ->
                ObjectStore.put(user)
            }
            preview.illusts.forEach { illust ->
                ObjectStore.put(illust)
                illust.user?.let { user -> ObjectStore.put(user) }
            }
        }
    }
}
