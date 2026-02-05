package ceui.lisa.jcstaff.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.Comment
import ceui.lisa.jcstaff.network.CommentResponse
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommentState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = null,
    val expandedReplies: Map<Long, List<Comment>> = emptyMap(),
    val loadingReplies: Set<Long> = emptySet(),
    val replyTarget: Comment? = null,
    val isPosting: Boolean = false,
    val isDeleting: Boolean = false
)

class CommentViewModel : ViewModel() {

    private val _state = MutableStateFlow(CommentState())
    val state: StateFlow<CommentState> = _state.asStateFlow()

    private var objectId: Long = 0
    private var objectType: String = "illust"
    private var isBound = false

    companion object {
        val commentsCache: MutableMap<String, List<Comment>> = mutableMapOf()

        fun cacheKey(objectType: String, objectId: Long) = "${objectType}_${objectId}"
    }

    fun loadComments(objectId: Long, objectType: String) {
        if (isBound) return
        this.objectId = objectId
        this.objectType = objectType
        this.isBound = true

        val cached = commentsCache[cacheKey(objectType, objectId)]
        if (cached != null) {
            _state.value = _state.value.copy(comments = cached)
        }

        fetchComments()
    }

    fun refresh() {
        fetchComments()
    }

    private fun fetchComments() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = if (objectType == "illust") {
                    PixivClient.pixivApi.getIllustComments(objectId)
                } else {
                    PixivClient.pixivApi.getNovelComments(objectId)
                }
                commentsCache[cacheKey(objectType, objectId)] = response.comments
                _state.value = _state.value.copy(
                    comments = response.comments,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadMore() {
        val nextUrl = _state.value.nextUrl ?: return
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val response = PixivClient.getNextPage(nextUrl, CommentResponse::class.java)
                val updated = _state.value.comments + response.comments
                commentsCache[cacheKey(objectType, objectId)] = updated
                _state.value = _state.value.copy(
                    comments = updated,
                    isLoading = false,
                    nextUrl = response.next_url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun expandReplies(commentId: Long) {
        if (_state.value.loadingReplies.contains(commentId)) return
        if (_state.value.expandedReplies.containsKey(commentId)) {
            // Collapse
            _state.value = _state.value.copy(
                expandedReplies = _state.value.expandedReplies - commentId
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                loadingReplies = _state.value.loadingReplies + commentId
            )
            try {
                val response = PixivClient.pixivApi.getCommentReplies(objectType, commentId)
                _state.value = _state.value.copy(
                    expandedReplies = _state.value.expandedReplies + (commentId to response.comments),
                    loadingReplies = _state.value.loadingReplies - commentId
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loadingReplies = _state.value.loadingReplies - commentId
                )
            }
        }
    }

    fun postComment(text: String, parentCommentId: Long? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPosting = true)
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
                        // Add to replies
                        val currentReplies = _state.value.expandedReplies[parentCommentId] ?: emptyList()
                        _state.value = _state.value.copy(
                            expandedReplies = _state.value.expandedReplies + (parentCommentId to (listOf(newComment) + currentReplies)),
                            isPosting = false,
                            replyTarget = null
                        )
                    } else {
                        val updated = listOf(newComment) + _state.value.comments
                        commentsCache[cacheKey(objectType, objectId)] = updated
                        _state.value = _state.value.copy(
                            comments = updated,
                            isPosting = false,
                            replyTarget = null
                        )
                    }
                } ?: run {
                    _state.value = _state.value.copy(isPosting = false, replyTarget = null)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isPosting = false)
                throw e
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)
            try {
                PixivClient.pixivApi.deleteComment(objectType, commentId)
                // Remove from top-level
                val updatedComments = _state.value.comments.filter { it.id != commentId }
                // Remove from replies
                val updatedReplies = _state.value.expandedReplies.mapValues { (_, replies) ->
                    replies.filter { it.id != commentId }
                }
                commentsCache[cacheKey(objectType, objectId)] = updatedComments
                _state.value = _state.value.copy(
                    comments = updatedComments,
                    expandedReplies = updatedReplies,
                    isDeleting = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDeleting = false)
                throw e
            }
        }
    }

    fun setReplyTarget(comment: Comment?) {
        _state.value = _state.value.copy(replyTarget = comment)
    }
}
