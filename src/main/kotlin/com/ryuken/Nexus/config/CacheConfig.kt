package com.ryuken.Nexus.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer()
        val serializationPair = RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(serializationPair)
            .disableCachingNullValues()

        val cacheConfigs = mapOf(
            "feed" to defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "trending" to defaultConfig.entryTtl(Duration.ofMinutes(15))
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }
}

