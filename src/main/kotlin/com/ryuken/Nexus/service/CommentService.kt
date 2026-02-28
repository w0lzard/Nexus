package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.CommentRepository
import com.ryuken.Nexus.database.repository.PostRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.AddCommentRequest
import com.ryuken.Nexus.dto.CommentResponse
import com.ryuken.Nexus.event.NotificationEvent
import com.ryuken.Nexus.model.Comment
import com.ryuken.Nexus.model.NotificationType
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun addComment(username: String, postId: UUID, request: AddCommentRequest): CommentResponse {
        val author = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }

        val parentComment = request.parentCommentId?.let {
            commentRepository.findById(it).orElseThrow { IllegalArgumentException("Parent comment not found") }
        }

        val sanitized = request.content.replace(Regex("<[^>]*>"), "")
        val comment = commentRepository.save(
            Comment(post = post, author = author, content = sanitized, parentComment = parentComment)
        )

        postRepository.incrementCommentCount(postId)

        if (post.author.id != author.id) {
            eventPublisher.publishEvent(
                NotificationEvent(
                    recipientId = post.author.id!!,
                    actorId = author.id!!,
                    type = NotificationType.COMMENT,
                    postId = postId,
                    commentId = comment.id
                )
            )
        }

        return comment.toCommentResponse()
    }

    @Transactional
    fun deleteComment(username: String, postId: UUID, commentId: UUID) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
        if (comment.author.username != username) throw IllegalArgumentException("Not authorized")
        commentRepository.delete(comment)
        postRepository.decrementCommentCount(postId)
    }

    fun getComments(postId: UUID, page: Int, size: Int): Page<CommentResponse> {
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }
        val pageable = PageRequest.of(page, size)
        return commentRepository.findByPostAndParentCommentIsNullOrderByCreatedAtAsc(post, pageable)
            .map { it.toCommentResponse() }
    }

    fun getReplies(commentId: UUID, page: Int, size: Int): Page<CommentResponse> {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
        val pageable = PageRequest.of(page, size)
        return commentRepository.findByParentCommentOrderByCreatedAtAsc(comment, pageable)
            .map { it.toCommentResponse() }
    }

    private fun Comment.toCommentResponse() = CommentResponse(
        id = this.id!!,
        author = this.author.toUserResponse(),
        content = this.content,
        parentCommentId = this.parentComment?.id,
        likeCount = this.likeCount,
        createdAt = this.createdAt
    )
}

