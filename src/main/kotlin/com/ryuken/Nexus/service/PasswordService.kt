package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.PasswordResetTokenRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.model.PasswordResetToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class PasswordService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String
) {

    fun changePassword(username: String, currentPassword: String, newPassword: String) {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
    }

    @Transactional
    fun forgotPassword(email: String) {
        val user = userRepository.findByEmail(email) ?: return // silent - don't reveal if email exists
        val token = UUID.randomUUID().toString()
        val expiry = Instant.now().plusSeconds(3600)
        passwordResetTokenRepository.save(PasswordResetToken(user = user, token = token, expiresAt = expiry))
        emailService.sendPasswordResetEmail(email, "$baseUrl/api/auth/reset-password?token=$token")
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid or expired token")
        if (resetToken.used) throw IllegalArgumentException("Token already used")
        if (resetToken.expiresAt.isBefore(Instant.now())) throw IllegalArgumentException("Token has expired")
        resetToken.user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(resetToken.user)
        resetToken.used = true
        passwordResetTokenRepository.save(resetToken)
    }
}

