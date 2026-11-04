package com.swiftroute.dto.response;

import java.time.Instant;

public record UrlClickResponse(
        Instant clickedAt,
        String referrer,
        String userAgent,
        String ipAddress
) {}
