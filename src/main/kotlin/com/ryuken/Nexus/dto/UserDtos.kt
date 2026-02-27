package com.ryuken.Nexus.dto

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(max = 100, message = "Display name must be at most 100 characters")
    val displayName: String? = null,

    @field:Size(max = 500, message = "Bio must be at most 500 characters")
    val bio: String? = null,

    val isPrivate: Boolean? = null
)

