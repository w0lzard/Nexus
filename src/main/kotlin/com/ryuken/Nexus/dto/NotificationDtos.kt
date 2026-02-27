package com.ryuken.Nexus.dto

import com.ryuken.Nexus.model.NotificationType
import java.time.Instant
import java.util.*

data class NotificationResponse(
    val id: UUID,
    val actorUsername: String,
    val actorAvatarUrl: String?,
    val type: NotificationType,
    val postId: UUID?,
    val commentId: UUID?,
    val isRead: Boolean,
    val createdAt: Instant?
)

data class UnreadCountResponse(
    val count: Long
)

