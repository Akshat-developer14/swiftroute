package com.swiftroute.dto.response;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Instant createdAt
) {}
