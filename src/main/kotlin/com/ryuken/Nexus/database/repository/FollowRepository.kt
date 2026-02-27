package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Follow
import com.ryuken.Nexus.model.FollowStatus
import com.ryuken.Nexus.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FollowRepository : JpaRepository<Follow, UUID> {

    fun findByFollowerAndFollowing(follower: User, following: User): Follow?

    fun countByFollowingAndStatus(following: User, status: FollowStatus): Long

    fun countByFollowerAndStatus(follower: User, status: FollowStatus): Long

    fun findByFollowingAndStatus(following: User, status: FollowStatus, pageable: Pageable): Page<Follow>

    fun findByFollowerAndStatus(follower: User, status: FollowStatus, pageable: Pageable): Page<Follow>

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId AND f.status = 'ACCEPTED'")
    fun findFollowingIds(@Param("userId") userId: UUID): List<UUID>

    fun existsByFollowerAndFollowingAndStatus(follower: User, following: User, status: FollowStatus): Boolean
}

