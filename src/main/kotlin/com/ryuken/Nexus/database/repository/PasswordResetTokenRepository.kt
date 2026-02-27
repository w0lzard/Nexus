package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {

    fun findByToken(token: String): PasswordResetToken?
}

