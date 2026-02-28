package com.ryuken.Nexus.service

import com.ryuken.Nexus.database.repository.PostRepository
import com.ryuken.Nexus.database.repository.ReportRepository
import com.ryuken.Nexus.database.repository.UserRepository
import com.ryuken.Nexus.dto.ReportResponse
import com.ryuken.Nexus.dto.SubmitReportRequest
import com.ryuken.Nexus.model.Report
import com.ryuken.Nexus.model.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) {

    @Transactional
    fun submitReport(reporterUsername: String, request: SubmitReportRequest): ReportResponse {
        val reporter = userRepository.findByUsername(reporterUsername)
            ?: throw IllegalArgumentException("User not found")
        if (request.reportedPostId == null && request.reportedUserId == null)
            throw IllegalArgumentException("Must specify a post or user to report")

        val reportedPost = request.reportedPostId?.let {
            postRepository.findById(it).orElseThrow { IllegalArgumentException("Post not found") }
        }
        val reportedUser = request.reportedUserId?.let {
            userRepository.findById(it).orElseThrow { IllegalArgumentException("User not found") }
        }

        val report = reportRepository.save(
            Report(reporter = reporter, reportedPost = reportedPost, reportedUser = reportedUser, reason = request.reason)
        )
        return report.toResponse()
    }

    fun listReports(status: ReportStatus, page: Int, size: Int): Page<ReportResponse> {
        val pageable = PageRequest.of(page, size)
        return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map { it.toResponse() }
    }

    @Transactional
    fun actionReport(reportId: UUID, status: ReportStatus) {
        val report = reportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("Report not found") }
        report.status = status
        reportRepository.save(report)
    }

    private fun Report.toResponse() = ReportResponse(
        id = this.id!!,
        reporterUsername = this.reporter.username,
        reportedPostId = this.reportedPost?.id,
        reportedUserId = this.reportedUser?.id,
        reason = this.reason,
        status = this.status.name,
        createdAt = this.createdAt
    )
}

