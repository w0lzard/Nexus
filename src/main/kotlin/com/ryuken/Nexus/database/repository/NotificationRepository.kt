package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Notification
import com.ryuken.Nexus.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {

    fun findByRecipientOrderByCreatedAtDesc(recipient: User, pageable: Pageable): Page<Notification>

    fun countByRecipientAndIsReadFalse(recipient: User): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :recipient")
    fun markAllReadByRecipient(@Param("recipient") recipient: User)
}

