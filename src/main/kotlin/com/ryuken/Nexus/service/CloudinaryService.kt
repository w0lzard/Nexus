package com.ryuken.Nexus.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryService(
    @Value("\${app.cloudinary.cloud-name}") cloudName: String,
    @Value("\${app.cloudinary.api-key}") apiKey: String,
    @Value("\${app.cloudinary.api-secret}") apiSecret: String
) : FileStorageService {

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to cloudName,
            "api_key" to apiKey,
            "api_secret" to apiSecret
        )
    )

    override fun uploadFile(file: MultipartFile, folder: String): String {
        val options = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "auto"
        )
        val result = cloudinary.uploader().upload(file.bytes, options)
        return result["secure_url"] as String
    }

    override fun deleteFile(publicId: String) {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
    }
}

