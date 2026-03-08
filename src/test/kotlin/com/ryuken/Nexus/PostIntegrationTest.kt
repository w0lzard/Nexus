package com.ryuken.Nexus

import com.fasterxml.jackson.databind.ObjectMapper
import com.ryuken.Nexus.config.TestRedisConfig
import com.ryuken.Nexus.dto.CreatePostRequest
import com.ryuken.Nexus.dto.RegisterRequest
import com.ryuken.Nexus.model.Visibility
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestRedisConfig::class)
class PostIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private var accessToken = ""
    private var userId = ""

    private fun register(username: String, email: String): Pair<String, String> {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(username = username, email = email, password = "password123")
            )
        }.andReturn()
        val tree = objectMapper.readTree(result.response.contentAsString)
        return tree["accessToken"].asText() to tree["user"]["id"].asText()
    }

    private fun createPost(token: String, postContent: String, visibility: Visibility = Visibility.PUBLIC): String {
        val body = objectMapper.writeValueAsString(CreatePostRequest(content = postContent, visibility = visibility))
        val result = mockMvc.post("/api/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = body
            header("Authorization", "Bearer $token")
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"]?.asText() ?: ""
    }

    @BeforeEach
    fun setup() {
        val (token, id) = register("postuser", "postuser@example.com")
        accessToken = token
        userId = id
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    fun `POST posts with valid body returns 201`() {
        mockMvc.post("/api/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "Hello world"))
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.content") { value("Hello world") }
            jsonPath("$.author.username") { value("postuser") }
        }
    }

    @Test
    fun `POST posts without JWT returns 403`() {
        mockMvc.post("/api/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "Hello"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST posts with content over 2000 chars returns 400`() {
        val longContent = "a".repeat(2001)
        mockMvc.post("/api/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = longContent))
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST posts with blank content and no files returns 400`() {
        mockMvc.post("/api/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"content":""}"""
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Test
    fun `GET post by id returns 200 for existing post`() {
        val postId = createPost(accessToken, "Test post")
        mockMvc.get("/api/posts/$postId").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(postId) }
        }
    }

    @Test
    fun `GET post by non-existent id returns 400`() {
        mockMvc.get("/api/posts/00000000-0000-0000-0000-000000000000").andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET public posts returns 200 without authentication`() {
        mockMvc.get("/api/posts/public").andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
        }
    }

    @Test
    fun `GET user posts returns paginated posts`() {
        createPost(accessToken, "Post 1")
        createPost(accessToken, "Post 2")
        mockMvc.get("/api/posts/user/postuser?page=0&size=20").andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun `DELETE post returns 204 for own post`() {
        val postId = createPost(accessToken, "My post to delete")
        mockMvc.delete("/api/posts/$postId") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE post returns 400 for another user's post`() {
        val postId = createPost(accessToken, "Someone else's post")
        val (otherToken, _) = register("otheruser", "other@example.com")

        mockMvc.delete("/api/posts/$postId") {
            header("Authorization", "Bearer $otherToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ── Feed ──────────────────────────────────────────────────────────────────

    @Test
    fun `GET feed without JWT returns 403`() {
        mockMvc.get("/api/posts/feed").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET feed with auth returns 200`() {
        mockMvc.get("/api/posts/feed") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
        }
    }

    @Test
    fun `GET feed for user with no follows returns public posts`() {
        createPost(accessToken, "A public post")
        mockMvc.get("/api/posts/feed") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
        }
    }

    @Test
    fun `GET feed does not include PRIVATE posts from followed users`() {
        val (otherToken, otherId) = register("privateauthor", "pvt@example.com")
        createPost(otherToken, "Private content", Visibility.PRIVATE)

        // Follow the other user
        mockMvc.post("/api/users/$otherId/follow") {
            header("Authorization", "Bearer $accessToken")
        }

        val result = mockMvc.get("/api/posts/feed") {
            header("Authorization", "Bearer $accessToken")
        }.andReturn()

        val posts = objectMapper.readTree(result.response.contentAsString)["content"]
        val hasPrivate = posts.any { it["visibility"].asText() == "PRIVATE" }
        assert(!hasPrivate) { "Feed should not contain PRIVATE posts" }
    }

    @Test
    fun `GET feed returns correct pagination metadata`() {
        mockMvc.get("/api/posts/feed?page=0&size=10") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.pageable") { exists() }
            jsonPath("$.totalElements") { exists() }
        }
    }
}
