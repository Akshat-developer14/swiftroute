package com.swiftroute.service;

import com.swiftroute.dto.helper.ClickMetadata;
import com.swiftroute.entity.Url;
import com.swiftroute.exception.UrlExpiredException;
import com.swiftroute.exception.UrlNotFoundException;
import com.swiftroute.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UrlService {
    private final UrlRepository urlRepository;
    private final UrlClickService urlClickService;

    public UrlService(UrlRepository urlRepository, UrlClickService urlClickService) {
        this.urlRepository = urlRepository;
        this.urlClickService = urlClickService;
    }

    public String getOriginalUrlAndLogClick(
            String shortCode,
            HttpServletRequest request
    ) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())) {
            throw new UrlExpiredException("This link expired on: " + url.getExpiresAt());
        }

        ClickMetadata metadata = ClickMetadata.builder()
                .url(url)
                .ipAddress(extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .referrer(request.getHeader("Referer"))
                .build();
        urlClickService.recordClick(metadata);

        return url.getOriginalUrl();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if(xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
