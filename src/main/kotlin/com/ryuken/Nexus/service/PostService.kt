package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.*
import com.ryuken.Nexus.dto.CreatePostRequest
import com.ryuken.Nexus.dto.PostResponse
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.model.Hashtag
import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.PostHashtag
import com.ryuken.Nexus.model.Visibility
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
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
    private val eventPublisher: ApplicationEventPublisher,
    private val likeService: LikeService,
    private val commentService: CommentService
) {

    @Transactional
    @CacheEvict(value = ["feed"], key = "#username")
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

        return saved.toPostResponse(author = author.toUserResponse())
    }

    fun getPostById(postId: UUID, viewerUsername: String?): PostResponse {
        val post = postRepository.findById(postId).orElseThrow { IllegalArgumentException("Post not found") }
        val viewer = viewerUsername?.let { userRepository.findByUsername(it) }
        val isLiked = viewer?.let { likeRepository.existsByUserAndPost(it, post) } ?: false
        return post.toPostResponse(isLiked = isLiked)
    }

    @Transactional
    @CacheEvict(value = ["feed"], allEntries = true)
    fun deletePost(postId: UUID, username: String) {
        val post = postRepository.findById(postId).orElseThrow { IllegalArgumentException("Post not found") }
        if (post.author.username != username) throw IllegalArgumentException("Not authorized to delete this post")
        post.parentPost?.let { postRepository.decrementCommentCount(it.id!!) }
        postRepository.delete(post)
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

    @Cacheable(value = ["feed"], key = "#username")
    fun getFeed(username: String, page: Int, size: Int): Page<PostResponse> {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        val followingIds = followRepository.findFollowingIds(user.id!!)
        if (followingIds.isEmpty()) return Page.empty()
        val pageable = PageRequest.of(page, size)
        return postRepository.findFeedForUser(followingIds, pageable).map { post ->
            val isLiked = likeRepository.existsByUserAndPost(user, post)
            post.toPostResponse(isLiked = isLiked)
        }
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

    private fun extractAndSaveHashtags(post: Post) {
        val content = post.content ?: return
        val tags = Regex("#(\\w+)").findAll(content).map { it.groupValues[1].lowercase() }.distinct()
        tags.forEach { tag ->
            val hashtag = hashtagRepository.findByTag(tag) ?: hashtagRepository.save(Hashtag(tag = tag))
            postHashtagRepository.save(PostHashtag(post = post, hashtag = hashtag))
        }
    }

    private fun sanitize(content: String?): String? {
        return content?.replace(Regex("<[^>]*>"), "")
    }

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
