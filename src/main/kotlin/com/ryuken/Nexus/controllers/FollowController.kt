package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.FollowResponse
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
    ): ResponseEntity<FollowResponse> {
        val response = followService.followUser(userDetails.username, id)
        return ResponseEntity.ok(response)
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
    ): ResponseEntity<Void> {
        followService.rejectFollowRequest(userDetails.username, followerId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/requests")
    fun getPendingRequests(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(followService.getPendingRequests(userDetails.username))
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

