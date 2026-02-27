package com.ryuken.Nexus.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class FollowResponse(
    val followerId: UUID,
    val followerUsername: String,
    val followerAvatarUrl: String?,
    val followingId: UUID,
    val followingUsername: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

data class SubmitReportRequest(
    val reportedPostId: UUID? = null,
    val reportedUserId: UUID? = null,

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 500, message = "Reason must be at most 500 characters")
    val reason: String
)

data class ReportResponse(
    val id: UUID,
    val reporterUsername: String,
    val reportedPostId: UUID?,
    val reportedUserId: UUID?,
    val reason: String,
    val status: String,
    val createdAt: java.time.Instant?
)

data class MessageResponse(
    val message: String
)

