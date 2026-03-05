package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Like
import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LikeRepository : JpaRepository<Like, UUID> {

    fun findByUserAndPost(user: User, post: Post): Like?

    fun existsByUserAndPost(user: User, post: Post): Boolean

    fun existsByUserIdAndPostId(userId: UUID, postId: UUID): Boolean

    fun countByPost(post: Post): Long

    /**
     * Returns the set of post IDs (from the given list) that a specific user has liked.
     * Use this for efficient bulk isLiked checks on a page of posts — one query instead of N.
     */
    @Query("SELECT l.post.id FROM Like l WHERE l.user.id = :userId AND l.post.id IN :postIds")
    fun findLikedPostIds(@Param("userId") userId: UUID, @Param("postIds") postIds: List<UUID>): Set<UUID>
}

