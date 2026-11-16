package com.swiftroute.controller;

import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlAnalyticsResponse;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.User;
import com.swiftroute.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/url")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping()
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal User user
            ) {
        UrlResponse urlResponse = urlService.createShortUrl(request, user);

        return ResponseEntity.ok(urlResponse);
    }

    @GetMapping("/my-urls")
    public ResponseEntity<List<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal User user
    ) {
        List<UrlResponse> urls = urlService.getMyUrls(user);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user
    ) {
        UrlAnalyticsResponse analytics = urlService.getUrlAnalytics(shortCode, user);
        return ResponseEntity.ok(analytics);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user
    ) {
        urlService.deleteUrl(shortCode, user);
        return ResponseEntity.noContent().build();
    }

}
