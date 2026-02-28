package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.CreatePostRequest
import com.ryuken.Nexus.dto.PostResponse
import com.ryuken.Nexus.service.PostService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    fun createPost(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestPart("post") request: CreatePostRequest,
        @RequestPart("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<PostResponse> {
        val response = postService.createPost(userDetails.username, request, files ?: emptyList())
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getPost(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.getPostById(id, userDetails?.username))
    }

    @DeleteMapping("/{id}")
    fun deletePost(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<Void> {
        postService.deletePost(id, userDetails.username)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/user/{username}")
    fun getUserPosts(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.getUserPosts(username, userDetails?.username, page, size))
    }

    @GetMapping("/public")
    fun getPublicPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.getPublicPosts(page, size))
    }

    @GetMapping("/feed")
    fun getFeed(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.getFeed(userDetails.username, page, size))
    }

    @GetMapping("/hashtag/{tag}")
    fun getByHashtag(
        @PathVariable tag: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.getPostsByHashtag(tag, page, size))
    }

    @GetMapping("/explore")
    fun getTrending(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.getTrending(page, size))
    }

    @PostMapping("/{id}/like")
    fun toggleLike(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<Map<String, Boolean>> {
        val liked = postService.toggleLike(userDetails.username, id)
        return ResponseEntity.ok(mapOf("liked" to liked))
    }

    @PostMapping("/{id}/comments")
    fun addComment(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: com.ryuken.Nexus.dto.AddCommentRequest
    ): ResponseEntity<com.ryuken.Nexus.dto.CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(postService.addComment(userDetails.username, id, request))
    }

    @GetMapping("/{id}/comments")
    fun getComments(
        @PathVariable id: java.util.UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<com.ryuken.Nexus.dto.CommentResponse>> {
        return ResponseEntity.ok(postService.getComments(id, page, size))
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: java.util.UUID,
        @PathVariable commentId: java.util.UUID
    ): ResponseEntity<Void> {
        postService.deleteComment(userDetails.username, id, commentId)
        return ResponseEntity.noContent().build()
    }
}

