package com.ryuken.Nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.ryuken.Nexus.dto.LoginRequest
import com.ryuken.Nexus.dto.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `register should return 201 with tokens`() {
        val request = RegisterRequest(
            username = "testuser",
            email = "testuser@example.com",
            password = "password123"
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
            jsonPath("$.user.username") { value("testuser") }
        }
    }

    @Test
    fun `register with duplicate username should return 400`() {
        val request = RegisterRequest(
            username = "testuser",
            email = "testuser@example.com",
            password = "password123"
        )
        // Register once
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
        // Register again with same username
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request.copy(email = "other@example.com"))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `register with invalid email should return 400`() {
        val request = RegisterRequest(
            username = "testuser2",
            email = "not-an-email",
            password = "password123"
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `login with valid credentials should return 200 with tokens`() {
        // Register first
        val register = RegisterRequest(
            username = "loginuser",
            email = "loginuser@example.com",
            password = "password123"
        )
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(register)
        }

        // Login
        val loginRequest = LoginRequest(usernameOrEmail = "loginuser", password = "password123")
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }
    }

    @Test
    fun `login with wrong password should return 401`() {
        val register = RegisterRequest(
            username = "badpassuser",
            email = "badpassuser@example.com",
            password = "password123"
        )
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(register)
        }

        val loginRequest = LoginRequest(usernameOrEmail = "badpassuser", password = "wrongpassword")
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `accessing protected endpoint without token should return 403`() {
        mockMvc.get("/api/users/me").andExpect {
            status { isForbidden() }
        }
    }
}

