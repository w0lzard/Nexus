package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.NotificationRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.NotificationResponse
import com.ryuken.Nexus.dto.UnreadCountResponse
import com.ryuken.Nexus.event.NotificationEvent
import com.ryuken.Nexus.model.Notification
import com.ryuken.Nexus.websocket.NotificationWebSocketService
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val webSocketService: NotificationWebSocketService
) {

    @Async
    @EventListener
    @Transactional
    fun handleNotificationEvent(event: NotificationEvent) {
        val recipient = userRepository.findById(event.recipientId).orElse(null) ?: return
        val actor = userRepository.findById(event.actorId).orElse(null) ?: return

        if (recipient.id == actor.id) return

        val notification = notificationRepository.save(
            Notification(
                recipient = recipient,
                actor = actor,
                type = event.type,
                postId = event.postId,
                commentId = event.commentId
            )
        )

        webSocketService.sendNotificationToUser(recipient.username, notification.toResponse())
    }

    fun getNotifications(username: String, page: Int, size: Int): Page<NotificationResponse> {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val pageable = PageRequest.of(page, size)
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
            .map { it.toResponse() }
    }

    fun getUnreadCount(username: String): UnreadCountResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        return UnreadCountResponse(notificationRepository.countByRecipientAndIsReadFalse(user))
    }

    @Transactional
    fun markAllRead(username: String) {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        notificationRepository.markAllReadByRecipient(user)
    }

    @Transactional
    fun markSingleRead(username: String, notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }
        if (notification.recipient.username != username) throw IllegalArgumentException("Not authorized")
        notification.isRead = true
        notificationRepository.save(notification)
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = this.id!!,
        actorUsername = this.actor.username,
        actorAvatarUrl = this.actor.avatarUrl,
        type = this.type,
        postId = this.postId,
        commentId = this.commentId,
        isRead = this.isRead,
        createdAt = this.createdAt
    )
}

