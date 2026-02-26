package com.ryuken.Nexus.model

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "hashtags")
class Hashtag(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 100)
    var tag: String
)

@Entity
@Table(
    name = "post_hashtags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "hashtag_id"])]
)
class PostHashtag(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    var hashtag: Hashtag
)

