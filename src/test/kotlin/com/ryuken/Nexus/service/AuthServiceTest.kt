package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.LoginRequest
import com.ryuken.Nexus.dto.RefreshTokenRequest
import com.ryuken.Nexus.dto.RegisterRequest
import com.ryuken.Nexus.util.JwtUtil
import com.ryuken.Nexus.util.TestUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @Mock lateinit var jwtUtil: JwtUtil
    @Mock lateinit var authenticationManager: AuthenticationManager

    @InjectMocks lateinit var authService: AuthService

    private val testUser = TestUtils.createTestUser()
    private val testUserId = testUser.id!!

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register with valid data saves user and returns AuthResponse with tokens`() {
        val request = RegisterRequest(username = "newuser", email = "new@example.com", password = "password123")
        `when`(userRepository.existsByUsername("newuser")).thenReturn(false)
        `when`(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        `when`(passwordEncoder.encode("password123")).thenReturn("hashed")
        `when`(userRepository.save(any())).thenReturn(testUser)
        `when`(jwtUtil.generateAccessToken(any(), any())).thenReturn("access-token")
        `when`(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token")

        val result = authService.register(request)

        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        verify(userRepository).save(any())
    }

    @Test
    fun `register with duplicate username throws IllegalArgumentException`() {
        val request = RegisterRequest(username = "testuser", email = "new@example.com", password = "password123")
        `when`(userRepository.existsByUsername("testuser")).thenReturn(true)

        assertThrows<IllegalArgumentException> { authService.register(request) }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register with duplicate email throws IllegalArgumentException`() {
        val request = RegisterRequest(username = "newuser", email = "testuser@example.com", password = "password123")
        `when`(userRepository.existsByUsername("newuser")).thenReturn(false)
        `when`(userRepository.existsByEmail("testuser@example.com")).thenReturn(true)

        assertThrows<IllegalArgumentException> { authService.register(request) }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register encodes the password before saving`() {
        val request = RegisterRequest(username = "newuser", email = "new@example.com", password = "plaintext")
        `when`(userRepository.existsByUsername(any())).thenReturn(false)
        `when`(userRepository.existsByEmail(any())).thenReturn(false)
        `when`(passwordEncoder.encode("plaintext")).thenReturn("bcrypt-hashed")
        `when`(userRepository.save(any())).thenReturn(testUser)
        `when`(jwtUtil.generateAccessToken(any(), any())).thenReturn("tok")
        `when`(jwtUtil.generateRefreshToken(any(), any())).thenReturn("ref")

        authService.register(request)

        verify(passwordEncoder).encode("plaintext")
    }

    @Test
    fun `register never saves plain text password`() {
        val request = RegisterRequest(username = "newuser", email = "new@example.com", password = "plaintext")
        `when`(userRepository.existsByUsername(any())).thenReturn(false)
        `when`(userRepository.existsByEmail(any())).thenReturn(false)
        `when`(passwordEncoder.encode("plaintext")).thenReturn("hashed-value")
        `when`(userRepository.save(any())).thenAnswer { invocation ->
            val savedUser = invocation.getArgument<com.ryuken.Nexus.model.User>(0)
            assertNotEquals("plaintext", savedUser.passwordHash)
            testUser
        }
        `when`(jwtUtil.generateAccessToken(any(), any())).thenReturn("tok")
        `when`(jwtUtil.generateRefreshToken(any(), any())).thenReturn("ref")

        authService.register(request)
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login with valid credentials returns AuthResponse with tokens`() {
        val request = LoginRequest(usernameOrEmail = "testuser", password = "password123")
        val auth = UsernamePasswordAuthenticationToken("testuser", null)
        `when`(authenticationManager.authenticate(any())).thenReturn(auth)
        `when`(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(testUser)
        `when`(jwtUtil.generateAccessToken(testUserId, "testuser")).thenReturn("access-tok")
        `when`(jwtUtil.generateRefreshToken(testUserId, "testuser")).thenReturn("refresh-tok")

        val result = authService.login(request)

        assertEquals("access-tok", result.accessToken)
        assertEquals("refresh-tok", result.refreshToken)
    }

    @Test
    fun `login with invalid credentials throws BadCredentialsException`() {
        val request = LoginRequest(usernameOrEmail = "testuser", password = "wrong")
        `when`(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Bad credentials"))

        assertThrows<BadCredentialsException> { authService.login(request) }
    }

    // ── refreshToken ──────────────────────────────────────────────────────────

    @Test
    fun `refreshToken with valid refresh token returns new AuthResponse`() {
        val request = RefreshTokenRequest(refreshToken = "valid-refresh")
        `when`(jwtUtil.validateToken("valid-refresh")).thenReturn(true)
        `when`(jwtUtil.getTokenType("valid-refresh")).thenReturn("refresh")
        `when`(jwtUtil.getUserIdFromToken("valid-refresh")).thenReturn(testUserId)
        `when`(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser))
        `when`(jwtUtil.generateAccessToken(testUserId, "testuser")).thenReturn("new-access")
        `when`(jwtUtil.generateRefreshToken(testUserId, "testuser")).thenReturn("new-refresh")

        val result = authService.refreshToken(request)

        assertEquals("new-access", result.accessToken)
    }

    @Test
    fun `refreshToken with access token instead of refresh type throws IllegalArgumentException`() {
        val request = RefreshTokenRequest(refreshToken = "access-token-mistakenly")
        `when`(jwtUtil.validateToken("access-token-mistakenly")).thenReturn(true)
        `when`(jwtUtil.getTokenType("access-token-mistakenly")).thenReturn("access")

        assertThrows<IllegalArgumentException> { authService.refreshToken(request) }
    }

    @Test
    fun `refreshToken with invalid token throws IllegalArgumentException`() {
        val request = RefreshTokenRequest(refreshToken = "bad-token")
        `when`(jwtUtil.validateToken("bad-token")).thenReturn(false)

        assertThrows<IllegalArgumentException> { authService.refreshToken(request) }
    }
}
