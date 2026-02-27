package com.ryuken.Nexus.database.repository

import com.ryuken.Nexus.model.Hashtag
import com.ryuken.Nexus.model.Post
import com.ryuken.Nexus.model.PostHashtag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface HashtagRepository : JpaRepository<Hashtag, UUID> {
    fun findByTag(tag: String): Hashtag?
}

@Repository
interface PostHashtagRepository : JpaRepository<PostHashtag, UUID> {
    fun findByPost(post: Post): List<PostHashtag>
}

