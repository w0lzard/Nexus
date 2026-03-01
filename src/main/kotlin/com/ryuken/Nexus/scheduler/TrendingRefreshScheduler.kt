package com.ryuken.Nexus.scheduler

import com.ryuken.Nexus.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TrendingRefreshScheduler(
    private val postService: PostService
) {
    private val logger = LoggerFactory.getLogger(TrendingRefreshScheduler::class.java)

    // Evict the trending cache every 15 minutes so the next request recomputes it
    @Scheduled(fixedRateString = "900000")
    @CacheEvict(value = ["trending"], allEntries = true)
    fun refreshTrendingCache() {
        logger.info("Trending cache evicted — will recompute on next request")
    }
}

