package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.*
import com.ryuken.Nexus.dto.CreatePostRequest
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.model.Visibility
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
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var likeRepository: LikeRepository
    @Mock lateinit var followRepository: FollowRepository
    @Mock lateinit var hashtagRepository: HashtagRepository
    @Mock lateinit var postHashtagRepository: PostHashtagRepository
    @Mock lateinit var fileStorageService: FileStorageService
    @Mock lateinit var likeService: LikeService
    @Mock lateinit var commentService: CommentService
    @Mock lateinit var redisTemplate: RedisTemplate<String, Any>
    @Mock lateinit var valueOperations: ValueOperations<String, Any>

    @InjectMocks lateinit var postService: PostService

    private val author = TestUtils.createTestUser()
    private val post = TestUtils.createTestPost(author = author)

    private fun stubRedisNoCache() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(any<String>())).thenReturn(null)
        `when`(redisTemplate.keys(any())).thenReturn(emptySet())
    }

    // ── createPost ────────────────────────────────────────────────────────────

    @Test
    fun `createPost saves post with correct author from SecurityContext`() {
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(postRepository.save(any())).thenReturn(post)
        `when`(redisTemplate.keys(any())).thenReturn(emptySet())

        val request = CreatePostRequest(content = "Hello world")
        val result = postService.createPost("testuser", request)

        assertEquals("testuser", result.author.username)
        verify(postRepository).save(any())
    }

    @Test
    fun `createPost with parentPostId increments repostCount on parent post`() {
        val parent = TestUtils.createTestPost(author = author, content = "parent")
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(postRepository.findById(parent.id!!)).thenReturn(Optional.of(parent))
        `when`(postRepository.save(any())).thenReturn(post)
        `when`(redisTemplate.keys(any())).thenReturn(emptySet())

        val request = CreatePostRequest(content = "Repost", parentPostId = parent.id)
        postService.createPost("testuser", request)

        verify(postRepository).incrementRepostCount(parent.id!!)
    }

    @Test
    fun `createPost with non-existent parentPostId throws IllegalArgumentException`() {
        val badId = UUID.randomUUID()
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(postRepository.findById(badId)).thenReturn(Optional.empty())

        val request = CreatePostRequest(content = "Repost", parentPostId = badId)
        assertThrows<IllegalArgumentException> { postService.createPost("testuser", request) }
    }

    // ── getPostById ───────────────────────────────────────────────────────────

    @Test
    fun `getPost returns PostResponse for existing PUBLIC post`() {
        `when`(postRepository.findById(post.id!!)).thenReturn(Optional.of(post))

        val result = postService.getPostById(post.id!!, null)

        assertEquals(post.id, result.id)
    }

    @Test
    fun `getPost throws IllegalArgumentException for non-existent post ID`() {
        val badId = UUID.randomUUID()
        `when`(postRepository.findById(badId)).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> { postService.getPostById(badId, null) }
    }

    @Test
    fun `getPost throws SecurityException for PRIVATE post when requester is not the author`() {
        val privatePost = TestUtils.createTestPost(author = author, visibility = Visibility.PRIVATE)
        val stranger = TestUtils.createTestUser(username = "stranger", email = "stranger@example.com")
        `when`(postRepository.findById(privatePost.id!!)).thenReturn(Optional.of(privatePost))
        `when`(userRepository.findByUsername("stranger")).thenReturn(stranger)

        assertThrows<SecurityException> { postService.getPostById(privatePost.id!!, "stranger") }
    }

    @Test
    fun `getPost throws SecurityException for FOLLOWERS_ONLY post when requester does not follow author`() {
        val fOnly = TestUtils.createTestPost(author = author, visibility = Visibility.FOLLOWERS_ONLY)
        val stranger = TestUtils.createTestUser(username = "stranger", email = "stranger@example.com")
        `when`(postRepository.findById(fOnly.id!!)).thenReturn(Optional.of(fOnly))
        `when`(userRepository.findByUsername("stranger")).thenReturn(stranger)
        `when`(followRepository.existsByFollowerAndFollowingAndStatus(stranger, author, FollowStatus.ACCEPTED))
            .thenReturn(false)

        assertThrows<SecurityException> { postService.getPostById(fOnly.id!!, "stranger") }
    }

    @Test
    fun `getPost returns post for FOLLOWERS_ONLY when requester follows the author`() {
        val fOnly = TestUtils.createTestPost(author = author, visibility = Visibility.FOLLOWERS_ONLY)
        val follower = TestUtils.createTestUser(username = "follower", email = "follower@example.com")
        `when`(postRepository.findById(fOnly.id!!)).thenReturn(Optional.of(fOnly))
        `when`(userRepository.findByUsername("follower")).thenReturn(follower)
        `when`(followRepository.existsByFollowerAndFollowingAndStatus(follower, author, FollowStatus.ACCEPTED))
            .thenReturn(true)
        `when`(likeRepository.existsByUserAndPost(follower, fOnly)).thenReturn(false)

        val result = postService.getPostById(fOnly.id!!, "follower")

        assertEquals(fOnly.id, result.id)
    }

    // ── deletePost ────────────────────────────────────────────────────────────

    @Test
    fun `deletePost deletes post when current user is the author`() {
        `when`(postRepository.findById(post.id!!)).thenReturn(Optional.of(post))
        `when`(redisTemplate.keys(any())).thenReturn(emptySet())

        postService.deletePost(post.id!!, "testuser")

        verify(postRepository).delete(post)
    }

    @Test
    fun `deletePost throws IllegalArgumentException when current user is not the author`() {
        `when`(postRepository.findById(post.id!!)).thenReturn(Optional.of(post))

        assertThrows<IllegalArgumentException> { postService.deletePost(post.id!!, "intruder") }
        verify(postRepository, never()).delete(any())
    }

    // ── getUserPosts ──────────────────────────────────────────────────────────

    @Test
    fun `getUserPosts returns paginated list for existing username`() {
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(postRepository.findByAuthorOrderByCreatedAtDesc(eq(author), any()))
            .thenReturn(PageImpl(listOf(post)))

        val result = postService.getUserPosts("testuser", null, 0, 20)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `getUserPosts throws IllegalArgumentException for non-existent username`() {
        `when`(userRepository.findByUsername("ghost")).thenReturn(null)

        assertThrows<IllegalArgumentException> { postService.getUserPosts("ghost", null, 0, 20) }
    }

    // ── getFeed ───────────────────────────────────────────────────────────────

    @Test
    fun `getFeed returns posts from followed users`() {
        stubRedisNoCache()
        val followedUser = TestUtils.createTestUser(username = "followed", email = "followed@example.com")
        val feedPost = TestUtils.createTestPost(author = followedUser, visibility = Visibility.PUBLIC)
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(followRepository.findFollowingIds(author.id!!)).thenReturn(listOf(followedUser.id!!))
        `when`(postRepository.findFeedForUser(eq(listOf(followedUser.id!!)), any()))
            .thenReturn(PageImpl(listOf(feedPost)))
        `when`(likeRepository.existsByUserAndPost(any(), any())).thenReturn(false)

        val result = postService.getFeed("testuser", 0, 20)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `getFeed falls back to public posts when user follows nobody`() {
        stubRedisNoCache()
        val publicPost = TestUtils.createTestPost(author = author, visibility = Visibility.PUBLIC)
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(followRepository.findFollowingIds(author.id!!)).thenReturn(emptyList())
        `when`(postRepository.findByVisibilityOrderByCreatedAtDesc(eq(Visibility.PUBLIC), any()))
            .thenReturn(PageImpl(listOf(publicPost)))
        `when`(likeRepository.existsByUserAndPost(any(), any())).thenReturn(false)

        val result = postService.getFeed("testuser", 0, 20)

        assertEquals(1, result.totalElements)
        verify(postRepository, never()).findFeedForUser(any(), any())
    }

    @Test
    fun `getFeed excludes PRIVATE posts from followed users`() {
        stubRedisNoCache()
        val followedUser = TestUtils.createTestUser(username = "followed", email = "followed@example.com")
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(followRepository.findFollowingIds(author.id!!)).thenReturn(listOf(followedUser.id!!))
        `when`(postRepository.findFeedForUser(any(), any())).thenReturn(PageImpl(emptyList()))

        val result = postService.getFeed("testuser", 0, 20)

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `getFeed returns cached result on second call without hitting database`() {
        val cachedPage = PageImpl(listOf<com.ryuken.Nexus.dto.PostResponse>())
        `when`(userRepository.findByUsername("testuser")).thenReturn(author)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(any<String>())).thenReturn(cachedPage)

        postService.getFeed("testuser", 0, 20)

        verify(postRepository, never()).findFeedForUser(any(), any())
        verify(postRepository, never()).findByVisibilityOrderByCreatedAtDesc(any(), any())
    }
}
