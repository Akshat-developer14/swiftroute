package com.swiftroute.service;

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

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
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

        return url.getOriginalUrl();
    }
}
