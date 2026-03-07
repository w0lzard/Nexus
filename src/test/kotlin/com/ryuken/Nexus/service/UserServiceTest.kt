package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.FollowRepository
import com.ryuken.Nexus.database.repository.PostRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.util.TestUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.springframework.web.multipart.MultipartFile

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var fileStorageService: FileStorageService
    @Mock lateinit var followRepository: FollowRepository
    @Mock lateinit var postRepository: PostRepository

    @InjectMocks lateinit var userService: UserService

    private val viewer = TestUtils.createTestUser(username = "viewer", email = "viewer@example.com")
    private val target = TestUtils.createTestUser(username = "target", email = "target@example.com")

    @Test
    fun `getCurrentUser returns UserResponse for authenticated user`() {
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(followRepository.countByFollowingAndStatus(viewer, FollowStatus.ACCEPTED)).thenReturn(5L)
        `when`(followRepository.countByFollowerAndStatus(viewer, FollowStatus.ACCEPTED)).thenReturn(10L)
        `when`(postRepository.countByAuthor(viewer)).thenReturn(3L)

        val result = userService.getCurrentUser("viewer")

        assertEquals("viewer", result.username)
        assertEquals(5L, result.followerCount)
        assertEquals(10L, result.followingCount)
        assertEquals(3L, result.postCount)
    }

    @Test
    fun `getProfile returns UserResponse for existing username`() {
        `when`(userRepository.findByUsername("target")).thenReturn(target)
        `when`(followRepository.countByFollowingAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(2L)
        `when`(followRepository.countByFollowerAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(7L)
        `when`(postRepository.countByAuthor(target)).thenReturn(1L)

        val result = userService.getUserByUsername(null, "target")

        assertEquals("target", result.username)
    }

    @Test
    fun `getProfile throws IllegalArgumentException for non-existent username`() {
        `when`(userRepository.findByUsername("ghost")).thenReturn(null)

        assertThrows<IllegalArgumentException> { userService.getUserByUsername(null, "ghost") }
    }

    @Test
    fun `updateProfile updates only non-null fields`() {
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(userRepository.save(any())).thenReturn(viewer)
        `when`(postRepository.countByAuthor(any())).thenReturn(0L)

        val request = com.ryuken.Nexus.dto.UpdateProfileRequest(bio = "New bio")
        userService.updateProfile("viewer", request)

        verify(userRepository).save(argThat { u -> u.bio == "New bio" })
    }

    @Test
    fun `updateProfile leaves displayName intact when only bio is provided`() {
        viewer.displayName = "Original Name"
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(userRepository.save(any())).thenAnswer { it.getArgument(0) }
        `when`(postRepository.countByAuthor(any())).thenReturn(0L)

        val request = com.ryuken.Nexus.dto.UpdateProfileRequest(bio = "New bio")
        userService.updateProfile("viewer", request)

        verify(userRepository).save(argThat { u -> u.displayName == "Original Name" && u.bio == "New bio" })
    }

    @Test
    fun `uploadAvatar calls uploadFile on FileStorageService and updates avatarUrl`() {
        val file = org.mockito.Mockito.mock(MultipartFile::class.java)
        `when`(fileStorageService.uploadFile(file, "avatars")).thenReturn("https://cdn.example.com/avatar.jpg")
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(userRepository.save(any())).thenReturn(viewer)
        `when`(postRepository.countByAuthor(any())).thenReturn(0L)

        userService.uploadAvatar("viewer", file)

        verify(fileStorageService).uploadFile(file, "avatars")
        verify(userRepository).save(argThat { u -> u.avatarUrl == "https://cdn.example.com/avatar.jpg" })
    }

    @Test
    fun `followerCount on UserResponse reflects accurate count from repository`() {
        `when`(userRepository.findByUsername("target")).thenReturn(target)
        `when`(followRepository.countByFollowingAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(42L)
        `when`(followRepository.countByFollowerAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(0L)
        `when`(postRepository.countByAuthor(target)).thenReturn(0L)

        val result = userService.getUserByUsername(null, "target")

        assertEquals(42L, result.followerCount)
    }

    @Test
    fun `followingCount on UserResponse reflects accurate count from repository`() {
        `when`(userRepository.findByUsername("target")).thenReturn(target)
        `when`(followRepository.countByFollowingAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(0L)
        `when`(followRepository.countByFollowerAndStatus(target, FollowStatus.ACCEPTED)).thenReturn(15L)
        `when`(postRepository.countByAuthor(target)).thenReturn(0L)

        val result = userService.getUserByUsername(null, "target")

        assertEquals(15L, result.followingCount)
    }

    @Test
    fun `isFollowing is true when current user follows the profile user`() {
        `when`(userRepository.findByUsername("target")).thenReturn(target)
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(followRepository.countByFollowingAndStatus(any(), any())).thenReturn(0L)
        `when`(followRepository.countByFollowerAndStatus(any(), any())).thenReturn(0L)
        `when`(postRepository.countByAuthor(any())).thenReturn(0L)
        `when`(followRepository.existsByFollowerAndFollowingAndStatus(viewer, target, FollowStatus.ACCEPTED))
            .thenReturn(true)

        val result = userService.getUserByUsername("viewer", "target")

        assertTrue(result.isFollowing)
    }

    @Test
    fun `isFollowing is false when current user does not follow the profile user`() {
        `when`(userRepository.findByUsername("target")).thenReturn(target)
        `when`(userRepository.findByUsername("viewer")).thenReturn(viewer)
        `when`(followRepository.countByFollowingAndStatus(any(), any())).thenReturn(0L)
        `when`(followRepository.countByFollowerAndStatus(any(), any())).thenReturn(0L)
        `when`(postRepository.countByAuthor(any())).thenReturn(0L)
        `when`(followRepository.existsByFollowerAndFollowingAndStatus(viewer, target, FollowStatus.ACCEPTED))
            .thenReturn(false)

        val result = userService.getUserByUsername("viewer", "target")

        assertFalse(result.isFollowing)
    }
}
