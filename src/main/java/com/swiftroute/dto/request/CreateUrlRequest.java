package com.swiftroute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank(message = "Original URL cannot be empty")
        @URL(message = "Must be a valid URL (e.g. https://example.com)")
        String originalUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,12}$", message = "Custom alias must be 3-12 alphanumeric characters, dashes, or underscores")
        String customAlias,

        Instant expiresAt
) {}
