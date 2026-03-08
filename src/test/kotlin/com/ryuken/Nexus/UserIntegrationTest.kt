package com.ryuken.Nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.ryuken.Nexus.config.TestRedisConfig
import com.ryuken.Nexus.dto.RegisterRequest
import com.ryuken.Nexus.dto.UpdateProfileRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private var accessToken = ""

    @BeforeAll
    fun setup() {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = "profileuser", email = "profile@example.com", password = "password123")
            )
        }.andReturn()
        accessToken = objectMapper.readTree(result.response.contentAsString)["accessToken"].asText()
    }

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Test
    fun `GET users-me with valid JWT returns 200 with user profile`() {
        mockMvc.get("/api/users/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("profileuser") }
            jsonPath("$.email") { value("profile@example.com") }
            jsonPath("$.followerCount") { exists() }
            jsonPath("$.followingCount") { exists() }
        }
    }

    @Test
    fun `GET users-me without JWT returns 403`() {
        mockMvc.get("/api/users/me").andExpect {
            status { isForbidden() }
        }
    }

    // ── GET /{username} ───────────────────────────────────────────────────────

    @Test
    fun `GET users-username returns 200 with profile`() {
        mockMvc.get("/api/users/profileuser").andExpect {
            status { isOk() }
            jsonPath("$.username") { value("profileuser") }
        }
    }

    @Test
    fun `GET users-username for non-existent user returns 400`() {
        mockMvc.get("/api/users/doesnotexist99").andExpect {
            status { isBadRequest() }
        }
    }

    // ── PUT /me ───────────────────────────────────────────────────────────────

    @Test
    fun `PUT users-me with valid body returns 200 with updated profile`() {
        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProfileRequest(displayName = "Updated Name", bio = "My bio"))
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.displayName") { value("Updated Name") }
            jsonPath("$.bio") { value("My bio") }
        }
    }

    @Test
    fun `PUT users-me updates only provided fields`() {
        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProfileRequest(displayName = "Initial Name"))
            header("Authorization", "Bearer $accessToken")
        }
        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProfileRequest(bio = "New bio only"))
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.displayName") { value("Initial Name") }
            jsonPath("$.bio") { value("New bio only") }
        }
    }

    @Test
    fun `PUT users-me with bio over 500 chars returns 400`() {
        val longBio = "b".repeat(501)
        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProfileRequest(bio = longBio))
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT users-me without JWT returns 403`() {
        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProfileRequest(bio = "test"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun `GET users-search returns matching users`() {
        mockMvc.get("/api/users/search?q=profile").andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
        }
    }
}
