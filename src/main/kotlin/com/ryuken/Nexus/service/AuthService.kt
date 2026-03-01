package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.*
import com.ryuken.Nexus.model.User
import com.ryuken.Nexus.util.JwtUtil
import com.ryuken.Nexus.util.toUserResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' is already taken")
        }

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email '${request.email}' is already in use")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password) ?: throw IllegalStateException("Password encoding failed"),
            displayName = request.displayName
        )

        val savedUser = userRepository.save(user)

        val accessToken = jwtUtil.generateAccessToken(savedUser.id!!, savedUser.username)
        val refreshToken = jwtUtil.generateRefreshToken(savedUser.id!!, savedUser.username)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = savedUser.toUserResponse()
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.usernameOrEmail, request.password)
        )

        val user = userRepository.findByUsernameOrEmail(request.usernameOrEmail, request.usernameOrEmail)
            ?: throw IllegalArgumentException("User not found")

        val accessToken = jwtUtil.generateAccessToken(user.id!!, user.username)
        val refreshToken = jwtUtil.generateRefreshToken(user.id!!, user.username)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user.toUserResponse()
        )
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        val token = request.refreshToken

        if (!jwtUtil.validateToken(token)) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        val tokenType = jwtUtil.getTokenType(token)
        if (tokenType != "refresh") {
            throw IllegalArgumentException("Invalid token type: expected refresh token")
        }

        val userId = jwtUtil.getUserIdFromToken(token)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val accessToken = jwtUtil.generateAccessToken(user.id!!, user.username)
        val newRefreshToken = jwtUtil.generateRefreshToken(user.id!!, user.username)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            user = user.toUserResponse()
        )
    }

}

