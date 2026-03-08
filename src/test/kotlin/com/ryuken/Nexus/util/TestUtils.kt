package com.ryuken.Nexus.util

import com.ryuken.Nexus.model.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.*

object TestUtils {

    private val testJwtSecret = "TestSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm2025!"
    private val testKey = Keys.hmacShaKeyFor(testJwtSecret.toByteArray())

    fun createTestUser(
        id: UUID = UUID.randomUUID(),
        username: String = "testuser",
        email: String = "testuser@example.com",
        passwordHash: String = "\$2a\$10\$hashedpassword",
        displayName: String? = "Test User",
        bio: String? = null,
        avatarUrl: String? = null,
        isPrivate: Boolean = false,
        isVerified: Boolean = false,
        role: Role = Role.USER
    ): User {
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl,
            isPrivate = isPrivate,
            isVerified = isVerified,
            role = role
        )
        // Reflectively set the id since it's normally set by JPA
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    fun createTestPost(
        id: UUID = UUID.randomUUID(),
        author: User = createTestUser(),
        content: String = "Test post content",
        visibility: Visibility = Visibility.PUBLIC,
        likeCount: Long = 0,
        commentCount: Long = 0,
        repostCount: Long = 0,
        parentPost: Post? = null
    ): Post {
        val post = Post(
            author = author,
            content = content,
            visibility = visibility,
            likeCount = likeCount,
            commentCount = commentCount,
            repostCount = repostCount,
            parentPost = parentPost
        )
        val idField = post.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(post, id)
        return post
    }

    fun createTestFollow(
        id: UUID = UUID.randomUUID(),
        follower: User = createTestUser(username = "follower", email = "follower@example.com"),
        following: User = createTestUser(username = "following", email = "following@example.com"),
        status: FollowStatus = FollowStatus.ACCEPTED
    ): Follow {
        val follow = Follow(follower = follower, following = following, status = status)
        val idField = follow.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(follow, id)
        return follow
    }

    fun generateTestJwt(
        userId: UUID,
        username: String,
        type: String = "access",
        expirationMs: Long = 900_000L
    ): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("type", type)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(testKey)
            .compact()
    }
}

