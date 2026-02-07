package ceui.lisa.jcstaff.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.PagedDataLoader
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.network.Comment
import ceui.lisa.jcstaff.network.CommentResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommentInteractionState(
    val expandedReplies: Map<Long, List<Comment>> = emptyMap(),
    val loadingReplies: Set<Long> = emptySet(),
    val replyTarget: Comment? = null,
    val isPosting: Boolean = false,
    val isDeleting: Boolean = false
)

class CommentViewModel(
    private val objectId: Long,
    private val objectType: String
) : ViewModel() {

    private val loader = PagedDataLoader(
        cacheConfig = cacheConfig(objectType, objectId),
        responseClass = CommentResponse::class.java,
        loadFirstPage = {
            if (objectType == "illust")
                PixivClient.pixivApi.getIllustComments(objectId)
            else
                PixivClient.pixivApi.getNovelComments(objectId)
        },
        itemFilter = ContentFilterManager::shouldShow,
        scope = viewModelScope
    )

    val pagedState: StateFlow<PagedState<Comment>> = loader.state

    private val _interactionState = MutableStateFlow(CommentInteractionState())
    val interactionState: StateFlow<CommentInteractionState> = _interactionState.asStateFlow()

    init {
        viewModelScope.launch { loader.load() }
    }

    fun loadMore() {
        viewModelScope.launch { loader.loadMore() }
    }

    fun refresh() {
        viewModelScope.launch { loader.refresh() }
    }

    fun expandReplies(commentId: Long) {
        val interaction = _interactionState.value
        if (interaction.loadingReplies.contains(commentId)) return
        if (interaction.expandedReplies.containsKey(commentId)) {
            _interactionState.value = interaction.copy(
                expandedReplies = interaction.expandedReplies - commentId
            )
            return
        }

        viewModelScope.launch {
            _interactionState.value = _interactionState.value.copy(
                loadingReplies = _interactionState.value.loadingReplies + commentId
            )
            try {
                val response = PixivClient.pixivApi.getCommentReplies(objectType, commentId)
                _interactionState.value = _interactionState.value.copy(
                    expandedReplies = _interactionState.value.expandedReplies + (commentId to response.comments),
                    loadingReplies = _interactionState.value.loadingReplies - commentId
                )
            } catch (e: Exception) {
                _interactionState.value = _interactionState.value.copy(
                    loadingReplies = _interactionState.value.loadingReplies - commentId
                )
            }
        }
    }

    fun postComment(text: String, parentCommentId: Long? = null) {
        viewModelScope.launch {
            _interactionState.value = _interactionState.value.copy(isPosting = true)
            try {
                val response = if (objectType == "illust") {
                    PixivClient.pixivApi.postIllustComment(
                        illustId = objectId,
                        comment = text,
                        parentCommentId = parentCommentId
                    )
                } else {
                    PixivClient.pixivApi.postNovelComment(
                        novelId = objectId,
                        comment = text,
                        parentCommentId = parentCommentId
                    )
                }
                response.comment?.let { newComment ->
                    if (parentCommentId != null) {
                        val currentReplies =
                            _interactionState.value.expandedReplies[parentCommentId] ?: emptyList()
                        _interactionState.value = _interactionState.value.copy(
                            expandedReplies = _interactionState.value.expandedReplies + (parentCommentId to (listOf(
                                newComment
                            ) + currentReplies)),
                            isPosting = false,
                            replyTarget = null
                        )
                    } else {
                        loader.updateItems { listOf(newComment) + it }
                        _interactionState.value = _interactionState.value.copy(
                            isPosting = false,
                            replyTarget = null
                        )
                    }
                } ?: run {
                    _interactionState.value = _interactionState.value.copy(
                        isPosting = false,
                        replyTarget = null
                    )
                }
            } catch (e: Exception) {
                _interactionState.value = _interactionState.value.copy(isPosting = false)
                e.printStackTrace()
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            _interactionState.value = _interactionState.value.copy(isDeleting = true)
            try {
                PixivClient.pixivApi.deleteComment(objectType, commentId)
                loader.updateItems { it.filter { c -> c.id != commentId } }
                val updatedReplies =
                    _interactionState.value.expandedReplies.mapValues { (_, replies) ->
                        replies.filter { it.id != commentId }
                    }
                _interactionState.value = _interactionState.value.copy(
                    expandedReplies = updatedReplies,
                    isDeleting = false
                )
            } catch (e: Exception) {
                _interactionState.value = _interactionState.value.copy(isDeleting = false)
                e.printStackTrace()
            }
        }
    }

    fun setReplyTarget(comment: Comment?) {
        _interactionState.value = _interactionState.value.copy(replyTarget = comment)
    }

    companion object {
        fun cacheConfig(objectType: String, objectId: Long) = CacheConfig(
            path = if (objectType == "illust") "/v3/illust/comments" else "/v3/novel/comments",
            queryParams = mapOf(
                (if (objectType == "illust") "illust_id" else "novel_id") to objectId.toString()
            )
        )

        fun factory(objectId: Long, objectType: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CommentViewModel(objectId, objectType) as T
            }
        }
    }
}
