package com.ryuken.Nexus.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

enum class NotificationType {
    LIKE, COMMENT, FOLLOW, FOLLOW_REQUEST, MENTION, REPOST
}

@Entity
@Table(name = "notifications")
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    var recipient: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    var actor: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: NotificationType,

    @Column(name = "post_id")
    var postId: UUID? = null,

    @Column(name = "comment_id")
    var commentId: UUID? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
)

