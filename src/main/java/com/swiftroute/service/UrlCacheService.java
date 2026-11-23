package com.swiftroute.service;

import com.swiftroute.dto.cache.CachedUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCacheService {
    private static final String KEY_PREFIX = "url:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // -- Public Method --

    public Optional<CachedUrl> get(String shortCode) {
        try {
            Object value = redisTemplate.opsForValue().get(KEY_PREFIX + shortCode);
            if (value instanceof CachedUrl cachedUrl) {
                return Optional.of(cachedUrl);
            }
            if (value != null) {
                return Optional.of(objectMapper.convertValue(value, CachedUrl.class));
            }
        } catch (Exception e) {
            log.warn("Redis read failure for shortCode {}: {}", shortCode, e.getMessage());
        }
        return Optional.empty();
    }

    public void put(String shortCode, CachedUrl cachedUrl) {
        try {
            Duration ttl = calculateTtl(cachedUrl.expiresAt());
            if (!ttl.isNegative() && !ttl.isZero()) {
                redisTemplate.opsForValue().set(KEY_PREFIX + shortCode, cachedUrl, ttl);
            }
        } catch (Exception e) {
            log.warn("Redis write failure for shortCode {}: {}", shortCode, e.getMessage());
        }
    }
    public void evict(String shortCode) {
        try {
            redisTemplate.delete(KEY_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Redis evict failure for shortCode {}: {}", shortCode, e.getMessage());
        }
    }

    // -- Private Method --

    private Duration calculateTtl(Instant expiresAt) {
        if (expiresAt == null) {
            return DEFAULT_TTL;
        }
        Duration timeUntilExpiry = Duration.between(Instant.now(), expiresAt);
        return timeUntilExpiry.isNegative() ? Duration.ZERO : timeUntilExpiry;
    }
}
