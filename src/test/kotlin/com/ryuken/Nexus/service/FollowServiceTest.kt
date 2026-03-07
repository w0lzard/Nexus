package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.FollowRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.model.Follow
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.util.TestUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.redis.core.RedisTemplate
import java.util.*

@ExtendWith(MockitoExtension::class)
class FollowServiceTest {

    @Mock lateinit var followRepository: FollowRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var eventPublisher: ApplicationEventPublisher
    @Mock lateinit var redisTemplate: RedisTemplate<String, Any>

    @InjectMocks lateinit var followService: FollowService

    private val follower = TestUtils.createTestUser(username = "follower", email = "follower@example.com")
    private val publicTarget = TestUtils.createTestUser(username = "public_target", email = "pub@example.com", isPrivate = false)
    private val privateTarget = TestUtils.createTestUser(username = "private_target", email = "priv@example.com", isPrivate = true)

    private fun stubRedisKeys() {
        `when`(redisTemplate.keys(any())).thenReturn(emptySet())
    }

    // ── follow ────────────────────────────────────────────────────────────────

    @Test
    fun `follow on a public account creates follow with ACCEPTED status`() {
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(publicTarget.id!!)).thenReturn(Optional.of(publicTarget))
        `when`(followRepository.findByFollowerAndFollowing(follower, publicTarget)).thenReturn(null)
        `when`(followRepository.save(any())).thenAnswer { it.getArgument(0) }
        stubRedisKeys()

        val result = followService.followUser("follower", publicTarget.id!!)

        assertEquals("ACCEPTED", result.status)
        assertEquals("Following", result.message)
    }

    @Test
    fun `follow on a private account creates follow with PENDING status`() {
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(privateTarget.id!!)).thenReturn(Optional.of(privateTarget))
        `when`(followRepository.findByFollowerAndFollowing(follower, privateTarget)).thenReturn(null)
        `when`(followRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = followService.followUser("follower", privateTarget.id!!)

        assertEquals("PENDING", result.status)
        assertEquals("Follow request sent", result.message)
    }

    @Test
    fun `follow when already following returns existing status without creating duplicate`() {
        val existing = TestUtils.createTestFollow(follower = follower, following = publicTarget, status = FollowStatus.ACCEPTED)
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(publicTarget.id!!)).thenReturn(Optional.of(publicTarget))
        `when`(followRepository.findByFollowerAndFollowing(follower, publicTarget)).thenReturn(existing)

        val result = followService.followUser("follower", publicTarget.id!!)

        assertEquals("ACCEPTED", result.status)
        verify(followRepository, never()).save(any())
    }

    @Test
    fun `follow on yourself throws IllegalArgumentException`() {
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(follower.id!!)).thenReturn(Optional.of(follower))

        assertThrows<IllegalArgumentException> { followService.followUser("follower", follower.id!!) }
    }

    @Test
    fun `follow on non-existent user throws IllegalArgumentException`() {
        val badId = UUID.randomUUID()
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(badId)).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> { followService.followUser("follower", badId) }
    }

    // ── unfollow ──────────────────────────────────────────────────────────────

    @Test
    fun `unfollow removes the follow record when it exists`() {
        val follow = TestUtils.createTestFollow(follower = follower, following = publicTarget)
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(publicTarget.id!!)).thenReturn(Optional.of(publicTarget))
        `when`(followRepository.findByFollowerAndFollowing(follower, publicTarget)).thenReturn(follow)
        stubRedisKeys()

        followService.unfollowUser("follower", publicTarget.id!!)

        verify(followRepository).delete(follow)
    }

    @Test
    fun `unfollow does nothing when follow record does not exist (idempotent)`() {
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(userRepository.findById(publicTarget.id!!)).thenReturn(Optional.of(publicTarget))
        `when`(followRepository.findByFollowerAndFollowing(follower, publicTarget)).thenReturn(null)

        assertDoesNotThrow { followService.unfollowUser("follower", publicTarget.id!!) }
        verify(followRepository, never()).delete(any<Follow>())
    }

    // ── acceptFollowRequest ───────────────────────────────────────────────────

    @Test
    fun `acceptFollowRequest changes status from PENDING to ACCEPTED`() {
        val pending = TestUtils.createTestFollow(follower = follower, following = privateTarget, status = FollowStatus.PENDING)
        `when`(userRepository.findByUsername("private_target")).thenReturn(privateTarget)
        `when`(userRepository.findById(follower.id!!)).thenReturn(Optional.of(follower))
        `when`(followRepository.findByFollowerAndFollowing(follower, privateTarget)).thenReturn(pending)
        `when`(followRepository.save(any())).thenAnswer { it.getArgument(0) }
        stubRedisKeys()

        followService.acceptFollowRequest("private_target", follower.id!!)

        verify(followRepository).save(org.mockito.kotlin.argThat { f -> f.status == FollowStatus.ACCEPTED })
    }

    @Test
    fun `acceptFollowRequest throws exception when no pending request exists`() {
        `when`(userRepository.findByUsername("private_target")).thenReturn(privateTarget)
        `when`(userRepository.findById(follower.id!!)).thenReturn(Optional.of(follower))
        `when`(followRepository.findByFollowerAndFollowing(follower, privateTarget)).thenReturn(null)

        assertThrows<IllegalArgumentException> { followService.acceptFollowRequest("private_target", follower.id!!) }
    }

    // ── rejectFollowRequest ───────────────────────────────────────────────────

    @Test
    fun `rejectFollowRequest deletes the follow record`() {
        val pending = TestUtils.createTestFollow(follower = follower, following = privateTarget, status = FollowStatus.PENDING)
        `when`(userRepository.findByUsername("private_target")).thenReturn(privateTarget)
        `when`(userRepository.findById(follower.id!!)).thenReturn(Optional.of(follower))
        `when`(followRepository.findByFollowerAndFollowing(follower, privateTarget)).thenReturn(pending)

        followService.rejectFollowRequest("private_target", follower.id!!)

        verify(followRepository).delete(pending)
    }

    // ── getFollowers / getFollowing ───────────────────────────────────────────

    @Test
    fun `getFollowers returns paginated list of followers`() {
        val follow = TestUtils.createTestFollow(follower = follower, following = publicTarget)
        `when`(userRepository.findById(publicTarget.id!!)).thenReturn(Optional.of(publicTarget))
        `when`(followRepository.findByFollowingAndStatus(eq(publicTarget), eq(FollowStatus.ACCEPTED), any()))
            .thenReturn(PageImpl(listOf(follow)))

        val result = followService.getFollowers(publicTarget.id!!, 0, 20)

        assertEquals(1, result.totalElements)
        assertEquals("follower", result.content[0].username)
    }

    @Test
    fun `getFollowing returns paginated list of following users`() {
        val follow = TestUtils.createTestFollow(follower = follower, following = publicTarget)
        `when`(userRepository.findById(follower.id!!)).thenReturn(Optional.of(follower))
        `when`(followRepository.findByFollowerAndStatus(eq(follower), eq(FollowStatus.ACCEPTED), any()))
            .thenReturn(PageImpl(listOf(follow)))

        val result = followService.getFollowing(follower.id!!, 0, 20)

        assertEquals(1, result.totalElements)
        assertEquals("public_target", result.content[0].username)
    }
}
