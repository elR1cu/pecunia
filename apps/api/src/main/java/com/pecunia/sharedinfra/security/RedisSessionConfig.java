package com.pecunia.sharedinfra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;

@Configuration
public class RedisSessionConfig {

    @Bean
    SessionRepositoryCustomizer<RedisSessionRepository> redisSessionNamespaceCustomizer(
            @Value("${spring.session.redis.namespace:pecunia:session}") String namespace) {
        return repository -> repository.setRedisKeyNamespace(namespace);
    }
}
