package com.ryuken.Nexus.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

enum class Visibility {
    PUBLIC, FOLLOWERS_ONLY, PRIVATE
}

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_author", columnList = "author_id"),
        Index(name = "idx_posts_created_at", columnList = "created_at DESC"),
        Index(name = "idx_posts_visibility", columnList = "visibility")
    ]
)
class Post(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: User,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @ElementCollection
    @CollectionTable(name = "post_media_urls", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "url")
    var mediaUrls: MutableList<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var visibility: Visibility = Visibility.PUBLIC,

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,

    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0,

    @Column(name = "repost_count", nullable = false)
    var repostCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    var parentPost: Post? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)

