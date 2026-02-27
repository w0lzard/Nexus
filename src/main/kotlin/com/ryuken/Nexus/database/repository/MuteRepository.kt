package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Mute
import com.ryuken.Nexus.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MuteRepository : JpaRepository<Mute, UUID> {

    fun findByMuterAndMuted(muter: User, muted: User): Mute?

    fun existsByMuterAndMuted(muter: User, muted: User): Boolean

    @Query("SELECT m.muted.id FROM Mute m WHERE m.muter.id = :userId")
    fun findMutedIds(@Param("userId") userId: UUID): List<UUID>
}

