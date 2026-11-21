package com.swiftroute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class CookieServiceTest {
    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "isSecure", true);
    }

    @Test
    void shouldCreateAccessTokenCookieWithSecurityFlags() {
        ResponseCookie cookie = cookieService.createAccessTokenCookie("my-jwt-token", 900L);

        assertEquals("access_token", cookie.getName());
        assertEquals("my-jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofSeconds(900L), cookie.getMaxAge());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void shouldCreateRefreshTokenCookieWithSecurityFlags() {
        ResponseCookie cookie = cookieService.createRefreshTokenCookie("my-jwt-token", 604_800L);

        assertEquals("refresh_token", cookie.getName());
        assertEquals("my-jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/api/v1/auth", cookie.getPath());
        assertEquals(Duration.ofSeconds(604_800L), cookie.getMaxAge());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void shouldCreateExpiredCookieToDeleteTokens() {
        // -- Access Cookie --

        ResponseCookie accessCookie = cookieService.deleteCookie("access_token", "/");

        assertEquals("access_token", accessCookie.getName());
        assertEquals("", accessCookie.getValue());
        assertTrue(accessCookie.isHttpOnly());
        assertTrue(accessCookie.isSecure());
        assertEquals("/", accessCookie.getPath());
        assertEquals(Duration.ofSeconds(0), accessCookie.getMaxAge());
        assertEquals("Strict", accessCookie.getSameSite());

        // -- Refresh Cookie --

        ResponseCookie refreshCookie = cookieService.deleteCookie("refresh_token", "/api/v1/auth");

        assertEquals("refresh_token", refreshCookie.getName());
        assertEquals("", refreshCookie.getValue());
        assertTrue(refreshCookie.isHttpOnly());
        assertTrue(refreshCookie.isSecure());
        assertEquals("/api/v1/auth", refreshCookie.getPath());
        assertEquals(Duration.ofSeconds(0), refreshCookie.getMaxAge());
        assertEquals("Strict", refreshCookie.getSameSite());
    }


}
