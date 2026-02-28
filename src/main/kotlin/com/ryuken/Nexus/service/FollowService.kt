package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.FollowRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.event.NotificationEvent
import com.ryuken.Nexus.model.Follow
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.model.NotificationType
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun followUser(followerUsername: String, followingId: UUID): String {
        val follower = userRepository.findByUsername(followerUsername)
            ?: throw IllegalArgumentException("User not found")
        val following = userRepository.findById(followingId)
            .orElseThrow { IllegalArgumentException("User to follow not found") }

        if (follower.id == following.id) throw IllegalArgumentException("Cannot follow yourself")

        val existing = followRepository.findByFollowerAndFollowing(follower, following)
        if (existing != null) throw IllegalArgumentException("Already following or request pending")

        val status = if (following.isPrivate) FollowStatus.PENDING else FollowStatus.ACCEPTED
        followRepository.save(Follow(follower = follower, following = following, status = status))

        val notifType = if (status == FollowStatus.PENDING) NotificationType.FOLLOW_REQUEST else NotificationType.FOLLOW
        eventPublisher.publishEvent(NotificationEvent(recipientId = following.id!!, actorId = follower.id!!, type = notifType))

        return if (status == FollowStatus.PENDING) "Follow request sent" else "Now following"
    }

    @Transactional
    fun unfollowUser(followerUsername: String, followingId: UUID) {
        val follower = userRepository.findByUsername(followerUsername)
            ?: throw IllegalArgumentException("User not found")
        val following = userRepository.findById(followingId)
            .orElseThrow { IllegalArgumentException("User not found") }
        val follow = followRepository.findByFollowerAndFollowing(follower, following)
            ?: throw IllegalArgumentException("Not following this user")
        followRepository.delete(follow)
    }

    @Transactional
    fun acceptFollowRequest(username: String, followerId: UUID) {
        val following = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val follower = userRepository.findById(followerId)
            .orElseThrow { IllegalArgumentException("Follower not found") }
        val follow = followRepository.findByFollowerAndFollowing(follower, following)
            ?: throw IllegalArgumentException("No pending request found")
        if (follow.status != FollowStatus.PENDING) throw IllegalArgumentException("No pending request")
        follow.status = FollowStatus.ACCEPTED
        followRepository.save(follow)
        eventPublisher.publishEvent(NotificationEvent(recipientId = follower.id!!, actorId = following.id!!, type = NotificationType.FOLLOW))
    }

    @Transactional
    fun rejectFollowRequest(username: String, followerId: UUID) {
        val following = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val follower = userRepository.findById(followerId)
            .orElseThrow { IllegalArgumentException("Follower not found") }
        val follow = followRepository.findByFollowerAndFollowing(follower, following)
            ?: throw IllegalArgumentException("No pending request found")
        followRepository.delete(follow)
    }

    fun getFollowers(userId: UUID, page: Int, size: Int): Page<UserResponse> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val pageable = PageRequest.of(page, size)
        return followRepository.findByFollowingAndStatus(user, FollowStatus.ACCEPTED, pageable)
            .map { it.follower.toUserResponse() }
    }

    fun getFollowing(userId: UUID, page: Int, size: Int): Page<UserResponse> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val pageable = PageRequest.of(page, size)
        return followRepository.findByFollowerAndStatus(user, FollowStatus.ACCEPTED, pageable)
            .map { it.following.toUserResponse() }
    }

    fun getFollowerCount(user: com.ryuken.Nexus.model.User): Long =
        followRepository.countByFollowingAndStatus(user, FollowStatus.ACCEPTED)

    fun getFollowingCount(user: com.ryuken.Nexus.model.User): Long =
        followRepository.countByFollowerAndStatus(user, FollowStatus.ACCEPTED)

    fun isFollowing(follower: com.ryuken.Nexus.model.User, following: com.ryuken.Nexus.model.User): Boolean =
        followRepository.existsByFollowerAndFollowingAndStatus(follower, following, FollowStatus.ACCEPTED)
}

