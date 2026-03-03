package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.*
import com.ryuken.Nexus.dto.CreatePostRequest
import com.ryuken.Nexus.dto.PostResponse
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.model.Hashtag
import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.PostHashtag
import com.ryuken.Nexus.model.Visibility
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.*

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val likeRepository: LikeRepository,
    private val followRepository: FollowRepository,
    private val hashtagRepository: HashtagRepository,
    private val postHashtagRepository: PostHashtagRepository,
    private val fileStorageService: FileStorageService,
    private val likeService: LikeService,
    private val commentService: CommentService,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    companion object {
        private const val FEED_KEY_PREFIX = "feed:"
        private val FEED_TTL = Duration.ofMinutes(2)
    }

    // ── Cache helpers ──────────────────────────────────────────────────────────

    private fun feedKey(userId: UUID, page: Int, size: Int) =
        "$FEED_KEY_PREFIX$userId:$page:$size"

    /** Delete all feed cache entries matching feed:* */
    fun evictAllFeedCaches() {
        val keys = redisTemplate.keys("$FEED_KEY_PREFIX*")
        if (!keys.isNullOrEmpty()) redisTemplate.delete(keys)
    }

    /** Delete only the feed cache entries for a specific user */
    fun evictFeedCacheForUser(userId: UUID) {
        val keys = redisTemplate.keys("$FEED_KEY_PREFIX$userId:*")
        if (!keys.isNullOrEmpty()) redisTemplate.delete(keys)
    }

    // ── Create / Delete ────────────────────────────────────────────────────────

    @Transactional
    fun createPost(username: String, request: CreatePostRequest, files: List<MultipartFile> = emptyList()): PostResponse {
        val author = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")

        if (request.content.isNullOrBlank() && files.isEmpty()) {
            throw IllegalArgumentException("Post must have content or media")
        }

        val mediaUrls = files.map { fileStorageService.uploadFile(it, "posts") }.toMutableList()

        val parentPost = request.parentPostId?.let {
            postRepository.findById(it).orElseThrow { IllegalArgumentException("Parent post not found") }
        }

        val post = Post(
            author = author,
            content = sanitize(request.content),
            mediaUrls = mediaUrls,
            visibility = request.visibility,
            parentPost = parentPost
        )
        val saved = postRepository.save(post)
        parentPost?.let { postRepository.incrementRepostCount(it.id!!) }
        extractAndSaveHashtags(saved)

        // Invalidate ALL feed caches — this author's followers' feeds are now stale
        evictAllFeedCaches()

        return saved.toPostResponse(author = author.toUserResponse())
    }

    @Transactional
    fun deletePost(postId: UUID, username: String) {
        val post = postRepository.findById(postId).orElseThrow { IllegalArgumentException("Post not found") }
        if (post.author.username != username) throw IllegalArgumentException("Not authorized to delete this post")
        post.parentPost?.let { postRepository.decrementCommentCount(it.id!!) }
        postRepository.delete(post)
        // Invalidate all feed caches so deleted post stops appearing
        evictAllFeedCaches()
    }

    // ── Feed ───────────────────────────────────────────────────────────────────

    fun getFeed(username: String, page: Int, size: Int): Page<PostResponse> {
        val safeSize = minOf(size, 50)
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")

        val cacheKey = feedKey(user.id!!, page, safeSize)

        // Try cache first
        @Suppress("UNCHECKED_CAST")
        val cached = redisTemplate.opsForValue().get(cacheKey) as? Page<PostResponse>
        if (cached != null) return cached

        // Cache miss — query the database
        val followingIds = followRepository.findFollowingIds(user.id!!)
        val pageable = PageRequest.of(page, safeSize)

        val result: Page<PostResponse> = if (followingIds.isEmpty()) {
            // New user fallback: show public posts so the feed is never blank
            postRepository.findByVisibilityOrderByCreatedAtDesc(Visibility.PUBLIC, pageable)
                .map { post ->
                    val isLiked = likeRepository.existsByUserAndPost(user, post)
                    post.toPostResponse(isLiked = isLiked)
                }
        } else {
            postRepository.findFeedForUser(followingIds, pageable).map { post ->
                val isLiked = likeRepository.existsByUserAndPost(user, post)
                post.toPostResponse(isLiked = isLiked)
            }
        }

        // Store in Redis with 2-minute TTL
        redisTemplate.opsForValue().set(cacheKey, result)
        redisTemplate.expire(cacheKey, FEED_TTL)

        return result
    }

    // ── Reads ──────────────────────────────────────────────────────────────────

    fun getPostById(postId: UUID, viewerUsername: String?): PostResponse {
        val post = postRepository.findById(postId).orElseThrow { IllegalArgumentException("Post not found") }
        val viewer = viewerUsername?.let { userRepository.findByUsername(it) }

        when (post.visibility) {
            Visibility.PRIVATE -> {
                if (viewer == null || viewer.id != post.author.id)
                    throw SecurityException("This post is private")
            }
            Visibility.FOLLOWERS_ONLY -> {
                if (viewer == null) throw SecurityException("This post is only visible to followers")
                if (viewer.id != post.author.id &&
                    !followRepository.existsByFollowerAndFollowingAndStatus(viewer, post.author, FollowStatus.ACCEPTED)
                ) throw SecurityException("This post is only visible to followers")
            }
            Visibility.PUBLIC -> { /* anyone can see */ }
        }

        val isLiked = viewer?.let { likeRepository.existsByUserAndPost(it, post) } ?: false
        return post.toPostResponse(isLiked = isLiked)
    }

    fun getUserPosts(targetUsername: String, viewerUsername: String?, page: Int, size: Int): Page<PostResponse> {
        val author = userRepository.findByUsername(targetUsername)
            ?: throw IllegalArgumentException("User not found")
        val viewer = viewerUsername?.let { userRepository.findByUsername(it) }
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return postRepository.findByAuthorOrderByCreatedAtDesc(author, pageable).map { post ->
            val isLiked = viewer?.let { likeRepository.existsByUserAndPost(it, post) } ?: false
            post.toPostResponse(isLiked = isLiked)
        }
    }

    fun getPublicPosts(page: Int, size: Int): Page<PostResponse> {
        val pageable = PageRequest.of(page, size)
        return postRepository.findByVisibilityOrderByCreatedAtDesc(Visibility.PUBLIC, pageable)
            .map { it.toPostResponse() }
    }

    fun getPostsByHashtag(tag: String, page: Int, size: Int): Page<PostResponse> {
        val pageable = PageRequest.of(page, size)
        return postRepository.findByHashtag(tag, pageable).map { it.toPostResponse() }
    }

    @Cacheable(value = ["trending"])
    fun getTrending(page: Int, size: Int): Page<PostResponse> {
        val pageable = PageRequest.of(page, size)
        return postRepository.findTrending(pageable).map { it.toPostResponse() }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun extractAndSaveHashtags(post: Post) {
        val content = post.content ?: return
        val tags = Regex("#(\\w+)").findAll(content).map { it.groupValues[1].lowercase() }.distinct()
        tags.forEach { tag ->
            val hashtag = hashtagRepository.findByTag(tag) ?: hashtagRepository.save(Hashtag(tag = tag))
            postHashtagRepository.save(PostHashtag(post = post, hashtag = hashtag))
        }
    }

    private fun sanitize(content: String?): String? =
        content?.replace(Regex("<[^>]*>"), "")

    fun Post.toPostResponse(author: UserResponse? = null, isLiked: Boolean = false): PostResponse {
        val authorResponse = author ?: this.author.toUserResponse()
        return PostResponse(
            id = this.id!!,
            author = authorResponse,
            content = this.content,
            mediaUrls = this.mediaUrls,
            visibility = this.visibility,
            likeCount = this.likeCount,
            commentCount = this.commentCount,
            repostCount = this.repostCount,
            isLiked = isLiked,
            parentPost = this.parentPost?.toPostResponse(),
            createdAt = this.createdAt
        )
    }

    fun toggleLike(username: String, postId: UUID): Boolean = likeService.toggleLike(username, postId)

    fun addComment(username: String, postId: UUID, request: com.ryuken.Nexus.dto.AddCommentRequest) =
        commentService.addComment(username, postId, request)

    fun deleteComment(username: String, postId: UUID, commentId: UUID) =
        commentService.deleteComment(username, postId, commentId)

    fun getComments(postId: UUID, page: Int, size: Int) =
        commentService.getComments(postId, page, size)
}

