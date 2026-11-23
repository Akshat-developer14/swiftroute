package com.swiftroute.service;

import com.swiftroute.dto.cache.CachedUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlCacheServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Mock private ValueOperations<String, Object> valueOperations;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private UrlCacheService urlCacheService;

    @BeforeEach
    void setUp() {
        // lenient() avoids UnnecessaryStubbingException in tests like evict() that don't call opsForValue()
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // -- Get Method Tests --

    @Test
    void get_WhenCachedUrlInstanceExists_ReturnsOptionalCachedUrl() {
        CachedUrl expected = new CachedUrl(1L, "https://example.com", null);
        when(valueOperations.get("url:short")).thenReturn(expected);

        Optional<CachedUrl> result = urlCacheService.get("short");

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
        verifyNoInteractions(objectMapper);
    }

    @Test
    void get_WhenRawMapReturned_ConvertsToCachedUrl() {
        Map<String, Object> rawMap = Map.of(
                "id", 1L,
                "originalUrl", "https://example.com"
        );
        when(valueOperations.get("url:short")).thenReturn(rawMap);

        Optional<CachedUrl> result = urlCacheService.get("short");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals("https://example.com", result.get().originalUrl());
    }

    @Test
    void get_WhenCacheMiss_ReturnsEmpty() {
        when(valueOperations.get("url:missing")).thenReturn(null);

        Optional<CachedUrl> result = urlCacheService.get("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void get_WhenRedisThrowsException_RedisDbDown() {
        when(valueOperations.get("url:short")).thenThrow(new RuntimeException("Redis down"));

        Optional<CachedUrl> result = urlCacheService.get("short");

        assertTrue(result.isEmpty());
    }

    // -- Put Method Tests --

    @Test
    void put_WhenExpiresAtIsNull_SetsWithDefault7DayTtl() {
        CachedUrl cachedUrl = new CachedUrl(1L, "https://example.com", null);

        urlCacheService.put("short", cachedUrl);

        verify(valueOperations).set("url:short", cachedUrl, Duration.ofDays(7));
    }

    @Test
    void put_WhenExpiresAtInFuture_SetsWithPositiveTtl() {
        Instant future = Instant.now().plusSeconds(3600);
        CachedUrl cachedUrl = new CachedUrl(1L, "https://example.com", future);

        urlCacheService.put("short", cachedUrl);

        verify(valueOperations).set(
                eq("url:short"),
                eq(cachedUrl),
                argThat((Duration ttl) -> ttl.getSeconds() > 3500 && ttl.getSeconds() <= 3600)
        );
    }

    @Test
    void put_WhenAlreadyExpired_DoesNotCallRedisSet() {
        Instant past = Instant.now().minusSeconds(60);
        CachedUrl cachedUrl = new CachedUrl(1L, "https://example.com", past);

        urlCacheService.put("short", cachedUrl);

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void put_WhenRedisThrowsException_DbDownOrUnknownReason() {
        CachedUrl cachedUrl = new CachedUrl(1L, "https://example.com", null);
        doThrow(new RuntimeException("Write failed"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        assertDoesNotThrow(() -> urlCacheService.put("short", cachedUrl));
    }

    // -- Evict Method Tests --

    @Test
    void evict_DeletesKeyWithPrefix() {
        urlCacheService.evict("short");

        verify(redisTemplate).delete("url:short");
    }

    @Test
    void evict_WhenRedisThrowsException_DbDownOrUnknownReason() {
        when(redisTemplate.delete("url:short")).thenThrow(new RuntimeException("Delete failed"));

        assertDoesNotThrow(() -> urlCacheService.evict("short"));
    }
}