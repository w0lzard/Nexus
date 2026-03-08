package com.ryuken.Nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.ryuken.Nexus.config.TestRedisConfig
import com.ryuken.Nexus.dto.LoginRequest
import com.ryuken.Nexus.dto.RefreshTokenRequest
import com.ryuken.Nexus.dto.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestRedisConfig::class)
class AuthIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun register(username: String, email: String, password: String = "password123"): String {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = username, email = email, password = password)
            )
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["accessToken"]?.asText() ?: ""
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    fun `POST register with valid body returns 201 with AuthResponse`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "newuser", email = "new@example.com", password = "password123")
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
            jsonPath("$.user.username") { value("newuser") }
            jsonPath("$.user.email") { value("new@example.com") }
        }
    }

    @Test
    fun `POST register with duplicate username returns 400`() {
        register("dupuser", "dup@example.com")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "dupuser", email = "other@example.com", password = "password123")
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { exists() }
        }
    }

    @Test
    fun `POST register with duplicate email returns 400`() {
        register("user1", "shared@example.com")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "user2", email = "shared@example.com", password = "password123")
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST register with blank username returns 400 with validation errors`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"","email":"ok@example.com","password":"password123"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.username") { exists() }
        }
    }

    @Test
    fun `POST register with username too short returns 400`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"ab","email":"ok@example.com","password":"password123"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.username") { exists() }
        }
    }

    @Test
    fun `POST register with invalid email returns 400 with validation errors`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"validuser","email":"not-an-email","password":"password123"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.email") { exists() }
        }
    }

    @Test
    fun `POST register with short password returns 400 with validation errors`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"validuser","email":"ok@example.com","password":"short"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.password") { exists() }
        }
    }

    @Test
    fun `POST register with non-alphanumeric username returns 400`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"bad user!","email":"ok@example.com","password":"password123"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.username") { exists() }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    fun `POST login with correct credentials returns 200 with tokens`() {
        register("loginuser", "login@example.com")
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(usernameOrEmail = "loginuser", password = "password123")
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }
    }

    @Test
    fun `POST login with wrong password returns 401`() {
        register("pwduser", "pwd@example.com")
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(usernameOrEmail = "pwduser", password = "wrongpassword")
            )
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST login with non-existent user returns 401`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(usernameOrEmail = "ghost", password = "password123")
            )
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `POST refresh with valid refresh token returns 200 with new access token`() {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "refreshuser", email = "refresh@example.com", password = "password123")
            )
        }.andReturn()
        val refreshToken = objectMapper.readTree(result.response.contentAsString)["refreshToken"].asText()

        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RefreshTokenRequest(refreshToken = refreshToken))
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }
    }

    @Test
    fun `POST refresh with access token returns 400`() {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "accuser", email = "acc@example.com", password = "password123")
            )
        }.andReturn()
        val accessToken = objectMapper.readTree(result.response.contentAsString)["accessToken"].asText()

        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RefreshTokenRequest(refreshToken = accessToken))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ── Protected endpoint ────────────────────────────────────────────────────

    @Test
    fun `GET users-me without JWT returns 403`() {
        mockMvc.get("/api/users/me").andExpect {
            status { isForbidden() }
        }
    }
}
