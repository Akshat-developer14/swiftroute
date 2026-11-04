package com.swiftroute.dto.response;

import java.time.Instant;

public record UrlResponse(
        String shortCode,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt
) {}
