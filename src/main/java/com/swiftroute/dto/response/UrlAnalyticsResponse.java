package com.swiftroute.dto.response;

import java.time.Instant;
import java.util.List;

public record UrlAnalyticsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks,
        Instant createdAt,
        Instant expiresAt,
        List<UrlClickResponse> recentClicks
) {}
