package com.swiftroute.controller;

import com.swiftroute.dto.helper.AuthTokensDto;
import com.swiftroute.dto.request.LoginRequest;
import com.swiftroute.dto.request.RegisterRequest;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.repository.UserRepository;
import com.swiftroute.service.AuthService;
import com.swiftroute.service.CookieService;
import com.swiftroute.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private CookieService cookieService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. /register
    @Test
    void register_ValidRequest_Returns201Created() throws Exception {
        RegisterRequest request = new RegisterRequest("Akshat", "akshat@gmail.com", "password123");
        UserResponse response = new UserResponse(1L, "Akshat", "akshat@gmail.com", Instant.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(response.email()));
    }

    @Test
    void register_InvalidRequest_Returns400BadRequest() throws Exception {
        // Blank/invalid fields to trigger @Valid failure before reaching AuthService
        RegisterRequest invalidRequest = new RegisterRequest(" ", "akshatgmail.com", "pass");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 2. /login
    @Test
    void login_ValidCredentials_Returns200AndSetsBothCookies() throws Exception {
        LoginRequest request = new LoginRequest("akshat@email.com", "password123");
        UserResponse userDto = new UserResponse(1L, "Akshat", "akshat@gmail.com", Instant.now());
        AuthTokensDto tokens = AuthTokensDto.builder()
                .accessToken("access-jwt")
                .refreshToken("refresh-jwt")
                .accessTokenExpirySeconds(900L)
                .refreshTokenExpirySeconds(604800L)
                .user(userDto)
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("access_token", "access-jwt").path("/").build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-jwt").path("/api/v1/auth").build();

        when(authService.login(any(LoginRequest.class))).thenReturn(tokens);
        when(cookieService.createAccessTokenCookie("access-jwt", 900L)).thenReturn(accessCookie);
        when(cookieService.createRefreshTokenCookie("refresh-jwt", 604800L)).thenReturn(refreshCookie);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access_token", "access-jwt"))
                .andExpect(cookie().value("refresh_token", "refresh-jwt"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_InvalidRequest_Returns400BadRequest() throws Exception {
        // Blank/invalid fields to trigger @Valid failure before reaching AuthService
        LoginRequest invalidRequest = new LoginRequest("akshatgmail.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService, cookieService);
    }

    // 3. /logout
    @Test
    void logout_ClearsBothCookiesAndReturns200() throws Exception {
        ResponseCookie cleanAccess = ResponseCookie.from("access_token", "").path("/").maxAge(0).build();
        ResponseCookie cleanRefresh = ResponseCookie.from("refresh_token", "").path("/api/v1/auth").maxAge(0).build();

        when(cookieService.deleteCookie("access_token", "/")).thenReturn(cleanAccess);
        when(cookieService.deleteCookie("refresh_token", "/api/v1/auth")).thenReturn(cleanRefresh);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // 4. /refresh
    @Test
    void refresh_WithRefreshCookie_SetsNewAccessCookieAndReturns200() throws Exception {
        ResponseCookie newAccessCookie = ResponseCookie.from("access_token", "new-access-jwt").path("/").build();

        when(authService.refreshAccessToken("old-refresh-jwt")).thenReturn("new-access-jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(cookieService.createAccessTokenCookie("new-access-jwt", 900L)).thenReturn(newAccessCookie);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh-jwt")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=new-access-jwt")))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }
}
