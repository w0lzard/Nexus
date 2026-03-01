package com.ryuken.Nexus.websocket

import com.ryuken.Nexus.dto.NotificationResponse
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class NotificationWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate
) {
    fun sendNotificationToUser(username: String, notification: NotificationResponse) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification)
    }
}

