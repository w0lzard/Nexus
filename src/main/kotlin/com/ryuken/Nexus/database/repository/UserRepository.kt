package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun findByUsernameOrEmail(username: String, email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}

