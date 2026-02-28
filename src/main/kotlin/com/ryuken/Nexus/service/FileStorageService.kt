package com.ryuken.Nexus.service

import org.springframework.web.multipart.MultipartFile

interface FileStorageService {
    fun uploadFile(file: MultipartFile, folder: String): String
    fun deleteFile(publicId: String)
}

