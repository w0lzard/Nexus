package com.ryuken.Nexus.dto

import com.ryuken.Nexus.model.Visibility
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

data class CreatePostRequest(
    @field:Size(max = 2000, message = "Content must be at most 2000 characters")
    val content: String? = null,

    val visibility: Visibility = Visibility.PUBLIC,

    val parentPostId: UUID? = null
)

data class PostResponse(
    val id: UUID,
    val author: UserResponse,
    val content: String?,
    val mediaUrls: List<String>,
    val visibility: Visibility,
    val likeCount: Long,
    val commentCount: Long,
    val repostCount: Long,
    val isLiked: Boolean = false,
    val parentPost: PostResponse? = null,
    val createdAt: Instant?,
    val updatedAt: Instant? = null
)

