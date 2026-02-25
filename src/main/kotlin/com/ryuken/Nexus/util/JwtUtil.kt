package com.ryuken.Nexus.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${app.jwt.secret}")
    private val secret: String,

    @Value("\${app.jwt.expiration-ms}")
    private val expirationMs: Long,

    @Value("\${app.jwt.refresh-expiration-ms}")
    private val refreshExpirationMs: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, username: String): String {
        return buildToken(userId, username, "access", expirationMs)
    }

    fun generateRefreshToken(userId: UUID, username: String): String {
        return buildToken(userId, username, "refresh", refreshExpirationMs)
    }

    private fun buildToken(userId: UUID, username: String, type: String, expirationMs: Long): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return UUID.fromString(claims.subject)
    }

    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims["username", String::class.java]
    }

    fun getTokenType(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims["type", String::class.java]
    }
}

