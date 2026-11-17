package com.swiftroute.dto.cache;

import java.io.Serializable;
import java.time.Instant;

public record CachedUrl(
        Long id,
        String originalUrl,
        Instant expiresAt
) implements Serializable {}
