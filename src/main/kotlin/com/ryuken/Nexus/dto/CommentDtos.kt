package com.ryuken.Nexus.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

data class AddCommentRequest(
    @field:NotBlank(message = "Comment content is required")
    @field:Size(max = 1000, message = "Comment must be at most 1000 characters")
    val content: String,

    val parentCommentId: UUID? = null
)

data class CommentResponse(
    val id: UUID,
    val author: UserResponse,
    val content: String,
    val parentCommentId: UUID?,
    val likeCount: Long,
    val createdAt: Instant?
)

