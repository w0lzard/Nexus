package com.ryuken.Nexus.util

import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.model.User

fun User.toUserResponse(
    followerCount: Long = 0,
    followingCount: Long = 0,
    postCount: Long = 0,
    isFollowing: Boolean = false
) = UserResponse(
    id = this.id!!,
    username = this.username,
    email = this.email,
    displayName = this.displayName,
    avatarUrl = this.avatarUrl,
    bio = this.bio,
    isPrivate = this.isPrivate,
    isVerified = this.isVerified,
    followerCount = followerCount,
    followingCount = followingCount,
    postCount = postCount,
    isFollowing = isFollowing,
    createdAt = this.createdAt
)

