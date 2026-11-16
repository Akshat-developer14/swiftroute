package com.swiftroute.service;

import com.swiftroute.dto.helper.ClickMetadata;
import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlAnalyticsResponse;
import com.swiftroute.dto.response.UrlClickResponse;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.Url;
import com.swiftroute.entity.UrlClick;
import com.swiftroute.entity.User;
import com.swiftroute.exception.AliasAlreadyTakenException;
import com.swiftroute.exception.UrlExpiredException;
import com.swiftroute.exception.UrlNotFoundException;
import com.swiftroute.mapper.UrlClickMapper;
import com.swiftroute.mapper.UrlMapper;
import com.swiftroute.repository.UrlClickRepository;
import com.swiftroute.repository.UrlRepository;
import com.swiftroute.util.Base62;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UrlService {
    private final UrlRepository urlRepository;
    private final UrlClickService urlClickService;
    private final UrlMapper urlMapper;
    private final UrlClickMapper urlClickMapper;
    private final UrlClickRepository urlClickRepository;

    public UrlService(UrlRepository urlRepository, UrlClickService urlClickService, UrlMapper urlMapper, UrlClickMapper urlClickMapper, UrlClickRepository urlClickRepository) {
        this.urlRepository = urlRepository;
        this.urlClickService = urlClickService;
        this.urlMapper = urlMapper;
        this.urlClickMapper = urlClickMapper;
        this.urlClickRepository = urlClickRepository;
    }

    public UrlAnalyticsResponse getUrlAnalytics(String shortCode, User user) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (!url.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }

        long totalClicks = urlClickRepository.countByUrlId(url.getId());

        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "clickedAt"));
        List<UrlClick> recentClicks = urlClickRepository.findByUrlId(url.getId(), top10).getContent();

        List<UrlClickResponse> clickResponses = urlClickMapper.toResponseList(recentClicks);

        return new UrlAnalyticsResponse(
                url.getShortCode(),
                url.getOriginalUrl(),
                totalClicks,
                url.getCreatedAt(),
                url.getExpiresAt(),
                clickResponses
        );
    }

    public List<UrlResponse> getMyUrls(User user) {
        List<Url> userUrls = urlRepository.findByUserId(user.getId());
        return urlMapper.toResponseList(userUrls);
    }

    @Transactional
    public void deleteUrl(String shortCode, User user) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (!url.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }

        urlClickRepository.deleteByUrlId(url.getId());
        urlRepository.delete(url);
    }

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {
        String shortCode;
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            String alias = request.customAlias().trim();

            if (urlRepository.existsByShortCode(alias)) {
                throw new AliasAlreadyTakenException("Custom alias '" + alias + "' is already in use");
            }

            shortCode = alias;
        } else {
            shortCode = generateUniqueShortCode();
        }

        Url url = new Url(
                request.originalUrl().trim(),
                shortCode,
                request.expiresAt(),
                user
        );

        Url savedUrl = urlRepository.save(url);
        return urlMapper.toResponse(savedUrl);
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

    private String generateUniqueShortCode() {
        String code;
        int maxAttempts = 5;
        int attempts = 0;

        do {
            code = Base62.generateRandomCode();
            attempts++;
            if (attempts >= maxAttempts) {
                throw new RuntimeException("Could not generate a unique short code, please try again");
            }
        } while (urlRepository.existsByShortCode(code));

        return code;
    }
}
