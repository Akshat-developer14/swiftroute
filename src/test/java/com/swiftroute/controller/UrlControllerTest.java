package com.swiftroute.controller;

import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlAnalyticsResponse;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.User;
import com.swiftroute.repository.UserRepository;
import com.swiftroute.service.JwtService;
import com.swiftroute.service.UrlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UrlService urlService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        testUser.setEmail("akshat@email.com");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. POST /api/v1/url
    @Test
    void createUrl_ValidRequest_Returns200AndUrlResponse() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com", "custom", null);
        UrlResponse response = new UrlResponse("custom", "https://example.com", null, Instant.now());

        when(urlService.createShortUrl(any(CreateUrlRequest.class), eq(testUser))).thenReturn(response);

        mockMvc.perform(post("/api/v1/url")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(response.shortCode()));
    }

    @Test
    void createUrl_InvalidRequest_Returns400BadRequest() throws Exception {
        CreateUrlRequest invalidRequest = new CreateUrlRequest("", null, null);

        mockMvc.perform(post("/api/v1/url")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlService);
    }

    // 2. GET /api/v1/url/my-urls
    @Test
    void getMyUrls_Returns200AndListOfUrls() throws Exception {
        List<UrlResponse> responses = List.of(
                new UrlResponse("custom", "https://example.com", null, Instant.now()),
                new UrlResponse("short", "https://example.com", null, Instant.now())
        );

        when(urlService.getMyUrls(testUser)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/url/my-urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // 3. GET /api/v1/url/{shortCode}/analytics
    @Test
    void getAnalytics_ValidShortCode_Returns200AndAnalytics() throws Exception {
        String shortCode = "short";
        UrlAnalyticsResponse analytics = new UrlAnalyticsResponse(
                shortCode, "https://example.com", 15L, Instant.now(), null, List.of()
        );

        when(urlService.getUrlAnalytics(shortCode, testUser)).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/url/{shortCode}/analytics", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(15));
    }

    // 4. DELETE /api/v1/url/{shortCode}
    @Test
    void deleteUrl_ValidShortCode_Returns204NoContent() throws Exception {
        String shortCode = "short";

        mockMvc.perform(delete("/api/v1/url/{shortCode}", shortCode)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(urlService).deleteUrl(shortCode, testUser);
    }
}
