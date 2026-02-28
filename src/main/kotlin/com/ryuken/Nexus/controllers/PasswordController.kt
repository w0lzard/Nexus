package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.ChangePasswordRequest
import com.ryuken.Nexus.dto.ForgotPasswordRequest
import com.ryuken.Nexus.dto.MessageResponse
import com.ryuken.Nexus.dto.ResetPasswordRequest
import com.ryuken.Nexus.service.PasswordService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class PasswordController(
    private val passwordService: PasswordService
) {

    @PutMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<MessageResponse> {
        passwordService.changePassword(userDetails.username, request.currentPassword, request.newPassword)
        return ResponseEntity.ok(MessageResponse("Password updated successfully"))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<MessageResponse> {
        passwordService.forgotPassword(request.email)
        return ResponseEntity.ok(MessageResponse("If that email exists, a reset link has been sent"))
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<MessageResponse> {
        passwordService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(MessageResponse("Password reset successfully"))
    }
}

