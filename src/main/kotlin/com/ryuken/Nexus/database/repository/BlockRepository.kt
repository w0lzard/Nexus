package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Block
import com.ryuken.Nexus.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BlockRepository : JpaRepository<Block, UUID> {

    fun findByBlockerAndBlocked(blocker: User, blocked: User): Block?

    fun existsByBlockerAndBlocked(blocker: User, blocked: User): Boolean

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    fun findBlockedIds(@Param("userId") userId: UUID): List<UUID>
}

