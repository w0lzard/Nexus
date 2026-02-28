package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.LikeRepository
import com.ryuken.Nexus.database.repository.PostRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.event.NotificationEvent
import com.ryuken.Nexus.model.Like
import com.ryuken.Nexus.model.NotificationType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class LikeService(
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun toggleLike(username: String, postId: UUID): Boolean {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }

        val existing = likeRepository.findByUserAndPost(user, post)
        return if (existing != null) {
            likeRepository.delete(existing)
            postRepository.decrementLikeCount(postId)
            false
        } else {
            likeRepository.save(Like(user = user, post = post))
            postRepository.incrementLikeCount(postId)
            if (post.author.id != user.id) {
                eventPublisher.publishEvent(
                    NotificationEvent(
                        recipientId = post.author.id!!,
                        actorId = user.id!!,
                        type = NotificationType.LIKE,
                        postId = postId
                    )
                )
            }
            true
        }
    }
}

