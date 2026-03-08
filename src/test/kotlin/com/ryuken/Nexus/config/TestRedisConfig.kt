package com.ryuken.Nexus.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate

@TestConfiguration
class TestRedisConfig {

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory =
        Mockito.mock(RedisConnectionFactory::class.java)

    @Bean(name = ["redisTemplate"])
    @Primary
    fun redisTemplate(): RedisTemplate<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return Mockito.mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
    }

    @Bean
    @Primary
    fun stringRedisTemplate(): StringRedisTemplate =
        Mockito.mock(StringRedisTemplate::class.java)
}
