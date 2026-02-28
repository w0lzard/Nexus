package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.MessageResponse
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.service.FollowService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users/{id}/follow")
class FollowController(
    private val followService: FollowService
) {

    @PostMapping
    fun follow(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        val message = followService.followUser(userDetails.username, id)
        return ResponseEntity.ok(MessageResponse(message))
    }

    @DeleteMapping
    fun unfollow(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        followService.unfollowUser(userDetails.username, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/accept/{followerId}")
    fun acceptRequest(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID,
        @PathVariable followerId: UUID
    ): ResponseEntity<MessageResponse> {
        followService.acceptFollowRequest(userDetails.username, followerId)
        return ResponseEntity.ok(MessageResponse("Follow request accepted"))
    }

    @DeleteMapping("/reject/{followerId}")
    fun rejectRequest(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID,
        @PathVariable followerId: UUID
    ): ResponseEntity<MessageResponse> {
        followService.rejectFollowRequest(userDetails.username, followerId)
        return ResponseEntity.ok(MessageResponse("Follow request rejected"))
    }

    @GetMapping("/followers")
    fun getFollowers(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<UserResponse>> {
        return ResponseEntity.ok(followService.getFollowers(id, page, size))
    }

    @GetMapping("/following")
    fun getFollowing(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<UserResponse>> {
        return ResponseEntity.ok(followService.getFollowing(id, page, size))
    }
}

