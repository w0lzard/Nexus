package com.ryuken.Nexus.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class CloudinaryService(
    @Value("\${app.cloudinary.cloud-name}") private val cloudName: String,
    @Value("\${app.cloudinary.api-key}") private val apiKey: String,
    @Value("\${app.cloudinary.api-secret}") apiSecretValue: String
) : FileStorageService {

    private val secret: String = apiSecretValue

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to cloudName,
            "api_key" to apiKey,
            "api_secret" to apiSecretValue
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

    fun generateUploadSignature(folder: String): Map<String, Any> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        // Build the string to sign: sorted params joined with & then append secret
        val toSign = "folder=$folder&timestamp=$timestamp$secret"
        val signature = hmacSha1Hex(toSign, secret)
        return mapOf(
            "signature" to signature,
            "timestamp" to timestamp,
            "apiKey" to apiKey,
            "cloudName" to cloudName,
            "folder" to folder
        )
    }

    private fun hmacSha1Hex(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

