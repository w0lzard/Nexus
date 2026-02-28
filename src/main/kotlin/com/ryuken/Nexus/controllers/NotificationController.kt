package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.NotificationResponse
import com.ryuken.Nexus.dto.UnreadCountResponse
import com.ryuken.Nexus.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<NotificationResponse>> {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.username, page, size))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<UnreadCountResponse> {
        return ResponseEntity.ok(notificationService.getUnreadCount(userDetails.username))
    }

    @PostMapping("/mark-all-read")
    fun markAllRead(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        notificationService.markAllRead(userDetails.username)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/read")
    fun markSingleRead(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        notificationService.markSingleRead(userDetails.username, id)
        return ResponseEntity.noContent().build()
    }
}

