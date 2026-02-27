package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Like
import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LikeRepository : JpaRepository<Like, UUID> {

    fun findByUserAndPost(user: User, post: Post): Like?

    fun existsByUserAndPost(user: User, post: Post): Boolean

    fun countByPost(post: Post): Long
}

