package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.FollowRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.FollowResponse
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.event.NotificationEvent
import com.ryuken.Nexus.model.Follow
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.model.NotificationType
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    private fun evictFeedCache(userId: UUID) {
        val keys = redisTemplate.keys("feed:$userId:*")
        if (!keys.isNullOrEmpty()) redisTemplate.delete(keys)
    }

    @Transactional
    fun followUser(followerUsername: String, followingId: UUID): FollowResponse {
        val follower = userRepository.findByUsername(followerUsername)
            ?: throw IllegalArgumentException("User not found")
        val following = userRepository.findById(followingId)
            .orElseThrow { IllegalArgumentException("User to follow not found") }

        if (follower.id == following.id) throw IllegalArgumentException("Cannot follow yourself")

        val existing = followRepository.findByFollowerAndFollowing(follower, following)
        if (existing != null) {
            val msg = if (existing.status == FollowStatus.PENDING) "Follow request sent" else "Following"
            return FollowResponse(status = existing.status.name, message = msg)
        }

        val status = if (following.isPrivate) FollowStatus.PENDING else FollowStatus.ACCEPTED
        followRepository.save(Follow(follower = follower, following = following, status = status))

        val notifType = if (status == FollowStatus.PENDING) NotificationType.FOLLOW_REQUEST else NotificationType.FOLLOW
        eventPublisher.publishEvent(NotificationEvent(recipientId = following.id!!, actorId = follower.id!!, type = notifType))

        // Invalidate follower's feed cache so the new person's posts appear immediately
        if (status == FollowStatus.ACCEPTED) evictFeedCache(follower.id!!)

        val message = if (status == FollowStatus.PENDING) "Follow request sent" else "Following"
        return FollowResponse(status = status.name, message = message)
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

        // Invalidate follower's feed cache so the unfollowed user's posts disappear immediately
        evictFeedCache(follower.id!!)
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

        // Invalidate follower's feed so the newly accepted account's posts appear
        evictFeedCache(follower.id!!)
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

    fun getPendingRequests(username: String): List<UserResponse> {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        return followRepository.findByFollowingAndStatus(user, FollowStatus.PENDING, PageRequest.of(0, 100))
            .content
            .map { it.follower.toUserResponse() }
    }
}


