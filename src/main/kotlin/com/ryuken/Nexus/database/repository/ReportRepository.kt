package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Report
import com.ryuken.Nexus.model.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReportRepository : JpaRepository<Report, UUID> {

    fun findByStatusOrderByCreatedAtDesc(status: ReportStatus, pageable: Pageable): Page<Report>
}

