package com.ryuken.Nexus.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingFilter : OncePerRequestFilter() {

    // Per-IP buckets for auth endpoints: 10 requests per minute
    private val authBuckets = ConcurrentHashMap<String, Bucket>()

    // Per-user buckets for post creation: 30 posts per hour
    private val postBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        val ip = getClientIp(request)

        when {
            path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register") -> {
                val bucket = authBuckets.computeIfAbsent(ip) { newAuthBucket() }
                if (!bucket.tryConsume(1)) {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    response.writer.write("""{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}""")
                    return
                }
            }
            path == "/api/posts" && request.method == "POST" -> {
                val principal = request.userPrincipal?.name ?: ip
                val bucket = postBuckets.computeIfAbsent(principal) { newPostBucket() }
                if (!bucket.tryConsume(1)) {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    response.writer.write("""{"status":429,"error":"Too Many Requests","message":"Post rate limit exceeded. Try again later."}""")
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun newAuthBucket(): Bucket =
        Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
            .build()

    private fun newPostBucket(): Bucket =
        Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofHours(1)).build())
            .build()

    private fun getClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) forwarded.split(",")[0].trim()
        else request.remoteAddr
    }
}

