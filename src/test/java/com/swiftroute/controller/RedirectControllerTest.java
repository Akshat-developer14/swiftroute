package com.swiftroute.controller;

import com.swiftroute.exception.UrlExpiredException;
import com.swiftroute.exception.UrlNotFoundException;
import com.swiftroute.repository.UserRepository;
import com.swiftroute.service.JwtService;
import com.swiftroute.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
@AutoConfigureMockMvc(addFilters = false)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void redirect_ValidShortCode_Returns302WithLocationHeader() throws Exception {
        String shortCode = "short";
        when(urlService.getOriginalUrlAndLogClick(eq(shortCode), any(HttpServletRequest.class)))
                .thenReturn("https://example.com/target");

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isFound()) // 302
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void redirect_WhenUrlNotFound_Returns404() throws Exception {
        String shortCode = "missing";
        when(urlService.getOriginalUrlAndLogClick(eq(shortCode), any(HttpServletRequest.class)))
                .thenThrow(new UrlNotFoundException("Short URL not found: " + shortCode));

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isNotFound()); // 404
    }

    @Test
    void redirect_WhenUrlExpired_Returns410() throws Exception {
        String shortCode = "expired";
        when(urlService.getOriginalUrlAndLogClick(eq(shortCode), any(HttpServletRequest.class)))
                .thenThrow(new UrlExpiredException("This link expired"));

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isGone()); // 410
    }
}