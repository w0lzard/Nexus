package com.ryuken.Nexus.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

enum class FollowStatus {
    PENDING, ACCEPTED
}

@Entity
@Table(
    name = "follows",
    uniqueConstraints = [UniqueConstraint(columnNames = ["follower_id", "following_id"])]
)
class Follow(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    var follower: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    var following: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: FollowStatus = FollowStatus.ACCEPTED,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
)

