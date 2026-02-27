package com.ryuken.Nexus.event

import com.ryuken.Nexus.model.NotificationType
import java.util.*

data class NotificationEvent(
    val recipientId: UUID,
    val actorId: UUID,
    val type: NotificationType,
    val postId: UUID? = null,
    val commentId: UUID? = null
)

