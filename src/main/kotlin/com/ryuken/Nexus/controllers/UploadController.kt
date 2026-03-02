package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.service.CloudinaryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/upload")
class UploadController(
    private val cloudinaryService: CloudinaryService
) {

    /**
     * Returns a signed upload signature the client uses to upload directly to Cloudinary.
     * The client POSTs the file to https://api.cloudinary.com/v1_1/{cloudName}/upload
     * including the signature, timestamp, apiKey, and folder returned here.
     *
     * Example usage:
     *   GET /api/upload/presign?folder=posts
     *   GET /api/upload/presign?folder=avatars
     */
    @GetMapping("/presign")
    fun getUploadSignature(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "posts") folder: String
    ): ResponseEntity<Map<String, Any>> {
        val allowedFolders = setOf("posts", "avatars")
        if (folder !in allowedFolders) {
            throw IllegalArgumentException("Invalid folder. Allowed: $allowedFolders")
        }
        return ResponseEntity.ok(cloudinaryService.generateUploadSignature(folder))
    }
}

