package com.swiftroute.controller;

import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlAnalyticsResponse;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.User;
import com.swiftroute.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "URLs", description = "URL shortening, management, and click analytics")
@RestController
@RequestMapping("/api/v1/url")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(summary = "Create short URL", description = "Generates a Base62 short URL or sets a custom alias with optional expiration.")
    @PostMapping()
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal User user
            ) {
        UrlResponse urlResponse = urlService.createShortUrl(request, user);

        return ResponseEntity.ok(urlResponse);
    }

    @Operation(summary = "Get user URLs", description = "Retrieves all short URLs created by the authenticated user.")
    @GetMapping("/my-urls")
    public ResponseEntity<List<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal User user
    ) {
        List<UrlResponse> urls = urlService.getMyUrls(user);
        return ResponseEntity.ok(urls);
    }

    @Operation(summary = "Get URL analytics", description = "Fetches total click counts and top 10 recent click metadata.")
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user
    ) {
        UrlAnalyticsResponse analytics = urlService.getUrlAnalytics(shortCode, user);
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Delete short URL", description = "Deletes the short URL, all associated click records, and evicts from Redis.")
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user
    ) {
        urlService.deleteUrl(shortCode, user);
        return ResponseEntity.noContent().build();
    }

}
