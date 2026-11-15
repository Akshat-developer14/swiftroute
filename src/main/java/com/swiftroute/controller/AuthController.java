package com.swiftroute.controller;

import com.swiftroute.dto.helper.AuthTokensDto;
import com.swiftroute.dto.request.LoginRequest;
import com.swiftroute.dto.request.RegisterRequest;
import com.swiftroute.dto.response.AuthResponse;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.service.AuthService;
import com.swiftroute.service.CookieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokensDto tokens = authService.login(request);

        ResponseCookie accessCookie = cookieService.createAccessTokenCookie(
                tokens.getAccessToken(),
                tokens.getAccessTokenExpirySeconds()
        );

        ResponseCookie refreshCookie = cookieService.createRefreshTokenCookie(
                tokens.getRefreshToken(),
                tokens.getRefreshTokenExpirySeconds()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(AuthResponse.of("Login successful", tokens.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        ResponseCookie cleanAccessCookie = cookieService.deleteCookie("access_token", "/");
        ResponseCookie cleanRefreshCookie = cookieService.deleteCookie("refresh_token", "/api/v1/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
                .body(AuthResponse.of("Logged out successfully", null));
    }
}
