package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.FollowRepository
import com.ryuken.Nexus.database.repository.PostRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.UpdateProfileRequest
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class UserService(
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
    private val followRepository: FollowRepository,
    private val postRepository: PostRepository
) {

    fun getCurrentUser(username: String): UserResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val followerCount = followRepository.countByFollowingAndStatus(user, FollowStatus.ACCEPTED)
        val followingCount = followRepository.countByFollowerAndStatus(user, FollowStatus.ACCEPTED)
        val postCount = postRepository.countByAuthor(user)
        return user.toUserResponse(followerCount = followerCount, followingCount = followingCount, postCount = postCount)
    }

    fun getUserByUsername(viewerUsername: String?, targetUsername: String): UserResponse {
        val target = userRepository.findByUsername(targetUsername)
            ?: throw IllegalArgumentException("User not found: $targetUsername")
        val followerCount = followRepository.countByFollowingAndStatus(target, FollowStatus.ACCEPTED)
        val followingCount = followRepository.countByFollowerAndStatus(target, FollowStatus.ACCEPTED)
        val postCount = postRepository.countByAuthor(target)
        val isFollowing = viewerUsername?.let { vn ->
            userRepository.findByUsername(vn)?.let { viewer ->
                followRepository.existsByFollowerAndFollowingAndStatus(viewer, target, FollowStatus.ACCEPTED)
            }
        } ?: false
        return target.toUserResponse(
            followerCount = followerCount,
            followingCount = followingCount,
            postCount = postCount,
            isFollowing = isFollowing
        )
    }

    fun updateProfile(username: String, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        request.displayName?.let { user.displayName = it }
        request.bio?.let { user.bio = it }
        request.isPrivate?.let { user.isPrivate = it }
        val saved = userRepository.save(user)
        val postCount = postRepository.countByAuthor(saved)
        return saved.toUserResponse(postCount = postCount)
    }

    fun updateAvatar(username: String, avatarUrl: String): UserResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        user.avatarUrl = avatarUrl
        val saved = userRepository.save(user)
        val postCount = postRepository.countByAuthor(saved)
        return saved.toUserResponse(postCount = postCount)
    }

    fun uploadAvatar(username: String, file: MultipartFile): UserResponse {
        val url = fileStorageService.uploadFile(file, "avatars")
        return updateAvatar(username, url)
    }

    fun searchUsers(query: String): List<UserResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, 50)
        return userRepository.searchUsers(query, pageable).content.map { it.toUserResponse() }
    }
}
