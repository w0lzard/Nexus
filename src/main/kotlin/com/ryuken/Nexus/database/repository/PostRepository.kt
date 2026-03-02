package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.User
import com.ryuken.Nexus.model.Visibility
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PostRepository : JpaRepository<Post, UUID> {

    fun findByAuthorOrderByCreatedAtDesc(author: User, pageable: Pageable): Page<Post>

    fun findByVisibilityOrderByCreatedAtDesc(visibility: Visibility, pageable: Pageable): Page<Post>

    @Query("SELECT p FROM Post p WHERE p.author.id IN :authorIds AND p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC")
    fun findFeedForUser(@Param("authorIds") authorIds: List<UUID>, pageable: Pageable): Page<Post>

    @Query("SELECT p FROM Post p JOIN PostHashtag ph ON ph.post = p JOIN Hashtag h ON ph.hashtag = h WHERE LOWER(h.tag) = LOWER(:tag) ORDER BY p.createdAt DESC")
    fun findByHashtag(@Param("tag") tag: String, pageable: Pageable): Page<Post>

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    fun incrementLikeCount(@Param("id") id: UUID)

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    fun decrementLikeCount(@Param("id") id: UUID)

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    fun incrementCommentCount(@Param("id") id: UUID)

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :id AND p.commentCount > 0")
    fun decrementCommentCount(@Param("id") id: UUID)

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :id")
    fun incrementRepostCount(@Param("id") id: UUID)

    @Query("SELECT p FROM Post p WHERE p.visibility = 'PUBLIC' ORDER BY (p.likeCount + p.commentCount * 2 + p.repostCount * 3) DESC, p.createdAt DESC")
    fun findTrending(pageable: Pageable): Page<Post>

    fun countByAuthor(author: User): Long
}

