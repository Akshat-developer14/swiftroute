package com.swiftroute.service;

import com.swiftroute.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {
    private JwtService jwtService;
    private User testUser;

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZyE";
    private static final Long ACCESS_TOKEN_EXPIRATION = 900_000L;
    private static final Long REFRESH_TOKEN_EXPIRATION = 604_800_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        testUser = new User();
        testUser.setEmail("aks@email.com");
        testUser.setName("Akshat Sharma");
        testUser.setPasswordHash("akshat");
        ReflectionTestUtils.setField(testUser, "id", 7021L);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String accessToken = jwtService.generateAccessToken(testUser);
        assertTrue(jwtService.validateToken(accessToken));
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);
        assertTrue(jwtService.validateToken(refreshToken));
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        JwtService tempJwtService1 = new JwtService();
        ReflectionTestUtils.setField(tempJwtService1, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(tempJwtService1, "accessTokenExpiration", 0L);
        ReflectionTestUtils.setField(tempJwtService1, "refreshTokenExpiration", 0L);

        String accessToken = tempJwtService1.generateAccessToken(testUser);
        assertFalse(jwtService.validateToken(accessToken));
    }

    @Test
    void shouldReturnFalseForTamperedOrMalformedToken() {
        JwtService tempJwtService2 = new JwtService();
        ReflectionTestUtils.setField(tempJwtService2, "secretKey", "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZye");
        ReflectionTestUtils.setField(tempJwtService2, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(tempJwtService2, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        String accessToken = tempJwtService2.generateAccessToken(testUser);
        assertFalse(jwtService.validateToken(accessToken));
    }

    @Test
    void shouldThrowExceptionWhenExtractingUserIdFromInvalidToken() {
        JwtService tempJwtService2 = new JwtService();
        ReflectionTestUtils.setField(tempJwtService2, "secretKey", "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZye");
        ReflectionTestUtils.setField(tempJwtService2, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(tempJwtService2, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        String accessToken = tempJwtService2.generateAccessToken(testUser);
        assertThrows(JwtException.class, () -> {
            jwtService.extractUserId(accessToken);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractUserId("");
        });
    }

    @Test
    void shouldReturnExpirationInSeconds() {
        assertEquals(900L, jwtService.getAccessTokenExpirationSeconds());
        assertEquals(604_800L, jwtService.getRefreshTokenExpirationSeconds());
    }


}
