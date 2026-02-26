package com.ryuken.Nexus.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "mutes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["muter_id", "muted_id"])]
)
class Mute(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muter_id", nullable = false)
    var muter: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muted_id", nullable = false)
    var muted: User,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
)

