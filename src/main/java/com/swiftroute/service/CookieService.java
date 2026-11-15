package com.swiftroute.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {
    @Value("${app.cookie.secure}")
    private boolean isSecure;

    public ResponseCookie createAccessTokenCookie(String token, long durationSeconds) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(durationSeconds))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token, long durationSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(durationSeconds))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie deleteCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isSecure)
                .path(path)
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}
