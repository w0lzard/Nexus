package com.ryuken.Nexus.controllers

import com.ryuken.Nexus.dto.ReportResponse
import com.ryuken.Nexus.dto.SubmitReportRequest
import com.ryuken.Nexus.model.ReportStatus
import com.ryuken.Nexus.service.ReportService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping
    fun submitReport(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: SubmitReportRequest
    ): ResponseEntity<ReportResponse> {
        val report = reportService.submitReport(userDetails.username, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(report)
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listReports(
        @RequestParam(defaultValue = "PENDING") status: ReportStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<ReportResponse>> {
        return ResponseEntity.ok(reportService.listReports(status, page, size))
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun actionReport(
        @PathVariable id: UUID,
        @RequestParam status: ReportStatus
    ): ResponseEntity<Void> {
        reportService.actionReport(id, status)
        return ResponseEntity.noContent().build()
    }
}

