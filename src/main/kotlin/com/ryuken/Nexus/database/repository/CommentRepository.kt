package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Comment
import com.ryuken.Nexus.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {

    fun findByPostAndParentCommentIsNullOrderByCreatedAtAsc(post: Post, pageable: Pageable): Page<Comment>

    fun findByParentCommentOrderByCreatedAtAsc(parentComment: Comment, pageable: Pageable): Page<Comment>

    fun countByPost(post: Post): Long
}

