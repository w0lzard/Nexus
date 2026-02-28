package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.MessageResponse
import com.ryuken.Nexus.service.BlockService
import com.ryuken.Nexus.service.MuteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
class BlockMuteController(
    private val blockService: BlockService,
    private val muteService: MuteService
) {

    @PostMapping("/{id}/block")
    fun block(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        blockService.blockUser(userDetails.username, id)
        return ResponseEntity.ok(MessageResponse("User blocked"))
    }

    @DeleteMapping("/{id}/block")
    fun unblock(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        blockService.unblockUser(userDetails.username, id)
        return ResponseEntity.ok(MessageResponse("User unblocked"))
    }

    @PostMapping("/{id}/mute")
    fun mute(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        muteService.muteUser(userDetails.username, id)
        return ResponseEntity.ok(MessageResponse("User muted"))
    }

    @DeleteMapping("/{id}/mute")
    fun unmute(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        muteService.unmuteUser(userDetails.username, id)
        return ResponseEntity.ok(MessageResponse("User unmuted"))
    }
}

