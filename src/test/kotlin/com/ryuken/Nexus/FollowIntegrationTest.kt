package com.ryuken.Nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.ryuken.Nexus.config.TestRedisConfig
import com.ryuken.Nexus.dto.RegisterRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestRedisConfig::class)
class FollowIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private var followerToken = ""
    private var followerId = ""
    private var publicTargetToken = ""
    private var publicTargetId = ""
    private var privateTargetToken = ""
    private var privateTargetId = ""

    private fun register(username: String, email: String, isPrivate: Boolean = false): Pair<String, String> {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = username, email = email, password = "password123")
            )
        }.andReturn()
        val tree = objectMapper.readTree(result.response.contentAsString)
        val token = tree["accessToken"].asText()
        val id = tree["user"]["id"].asText()

        if (isPrivate) {
            mockMvc.put("/api/users/me") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"isPrivate":true}"""
                header("Authorization", "Bearer $token")
            }
        }
        return token to id
    }

    @BeforeEach
    fun setup() {
        val (ft, fi) = register("followeruser", "follower@example.com")
        followerToken = ft; followerId = fi

        val (pt, pi) = register("publictarget", "public@example.com")
        publicTargetToken = pt; publicTargetId = pi

        val (pvt, pvi) = register("privatetarget", "private@example.com", isPrivate = true)
        privateTargetToken = pvt; privateTargetId = pvi
    }

    // ── Follow ────────────────────────────────────────────────────────────────

    @Test
    fun `POST follow returns 200 with ACCEPTED for public account`() {
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ACCEPTED") }
        }
    }

    @Test
    fun `POST follow returns 200 with PENDING for private account`() {
        mockMvc.post("/api/users/$privateTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `POST follow on yourself returns 400`() {
        mockMvc.post("/api/users/$followerId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST follow twice does not create duplicate and returns existing status`() {
        // Follow once
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect { status { isOk() } }

        // Follow again — should return same status, not 500 or duplicate key error
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ACCEPTED") }
        }
    }

    @Test
    fun `POST follow without JWT returns 403`() {
        mockMvc.post("/api/users/$publicTargetId/follow").andExpect {
            status { isForbidden() }
        }
    }

    // ── Unfollow ──────────────────────────────────────────────────────────────

    @Test
    fun `DELETE follow returns 204`() {
        // Follow first
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }

        mockMvc.delete("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE follow when not following returns 204 (idempotent)`() {
        mockMvc.delete("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }.andExpect {
            status { isNoContent() }
        }
    }

    // ── Followers / Following ─────────────────────────────────────────────────

    @Test
    fun `GET followers returns paginated followers`() {
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }

        mockMvc.get("/api/users/$publicTargetId/follow/followers?page=0&size=20")
            .andExpect {
                status { isOk() }
                jsonPath("$.content") { isArray() }
            }
    }

    @Test
    fun `GET following returns paginated following users`() {
        mockMvc.post("/api/users/$publicTargetId/follow") {
            header("Authorization", "Bearer $followerToken")
        }

        mockMvc.get("/api/users/$followerId/follow/following?page=0&size=20")
            .andExpect {
                status { isOk() }
                jsonPath("$.content") { isArray() }
            }
    }
}

