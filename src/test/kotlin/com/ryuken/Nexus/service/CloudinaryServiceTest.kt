package com.ryuken.Nexus.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.web.multipart.MultipartFile

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudinaryServiceTest {

    private fun service() = CloudinaryService(
        cloudName = "test-cloud",
        apiKey = "test-key",
        apiSecretValue = "test-secret"
    )

    // ── File type validation ──────────────────────────────────────────────────

    @Test
    fun `uploadFile with pdf content type throws IllegalArgumentException`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("application/pdf")
        `when`(file.size).thenReturn(1024L)

        assertThrows<IllegalArgumentException> { service().uploadFile(file, "posts") }
    }

    @Test
    fun `uploadFile with file over 10MB throws IllegalArgumentException`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("image/jpeg")
        `when`(file.size).thenReturn(11L * 1024L * 1024L)

        assertThrows<IllegalArgumentException> { service().uploadFile(file, "posts") }
    }

    @Test
    fun `uploadFile with missing content type throws IllegalArgumentException`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn(null)
        `when`(file.size).thenReturn(1024L)

        assertThrows<IllegalArgumentException> { service().uploadFile(file, "posts") }
    }

    @Test
    fun `uploadFile with jpeg passes type validation but fails on Cloudinary network call`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("image/jpeg")
        `when`(file.size).thenReturn(500_000L)
        `when`(file.bytes).thenReturn(ByteArray(0))

        // Passes type/size validation; then fails at Cloudinary SDK network call — not IllegalArgumentException
        val thrown = assertThrows<Exception> { service().uploadFile(file, "posts") }
        assertFalse(thrown is IllegalArgumentException,
            "jpeg should pass validation — exception must be network/SDK-related, not IllegalArgumentException")
    }

    @Test
    fun `uploadFile with png passes type validation but fails on Cloudinary network call`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("image/png")
        `when`(file.size).thenReturn(500_000L)
        `when`(file.bytes).thenReturn(ByteArray(0))

        val thrown = assertThrows<Exception> { service().uploadFile(file, "posts") }
        assertFalse(thrown is IllegalArgumentException,
            "png should pass validation — exception must be network/SDK-related, not IllegalArgumentException")
    }

    @Test
    fun `uploadFile with webp passes type validation`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("image/webp")
        `when`(file.size).thenReturn(200_000L)
        `when`(file.bytes).thenReturn(ByteArray(0))

        val thrown = assertThrows<Exception> { service().uploadFile(file, "posts") }
        assertFalse(thrown is IllegalArgumentException)
    }

    @Test
    fun `uploadFile with exactly 10MB does not throw size exception`() {
        val file = mock(MultipartFile::class.java)
        `when`(file.contentType).thenReturn("image/jpeg")
        `when`(file.size).thenReturn(10L * 1024L * 1024L) // exactly 10MB — allowed
        `when`(file.bytes).thenReturn(ByteArray(0))

        val thrown = assertThrows<Exception> { service().uploadFile(file, "posts") }
        assertFalse(thrown is IllegalArgumentException,
            "Exactly 10MB should be allowed — not an IllegalArgumentException")
    }
}
