package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.UpdateProfileRequest
import com.ryuken.Nexus.dto.UserResponse
import com.ryuken.Nexus.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<UserResponse> {
        val response = userService.getCurrentUser(userDetails.username)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{username}")
    fun getUserByUsername(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @PathVariable username: String
    ): ResponseEntity<UserResponse> {
        val response = userService.getUserByUsername(userDetails?.username, username)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val response = userService.updateProfile(userDetails.username, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search")
    fun searchUsers(@RequestParam q: String): ResponseEntity<List<UserResponse>> {
        val results = userService.searchUsers(q)
        return ResponseEntity.ok(results)
    }

    @PostMapping("/me/avatar", consumes = ["multipart/form-data"])
    fun uploadAvatar(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UserResponse> {
        val response = userService.uploadAvatar(userDetails.username, file)
        return ResponseEntity.ok(response)
    }
}



