package com.swiftroute.filter;

import com.swiftroute.entity.User;
import com.swiftroute.repository.UserRepository;
import com.swiftroute.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ValidAccessTokenCookie_SetsAuthenticationAndContinuesChain() throws Exception {
        String token = "valid.jwt.token";
        request.setCookies(new Cookie("access_token", token));

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        when(jwtService.extractUserId(token)).thenReturn(1L);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(user, auth.getPrincipal());
        assertTrue(auth.isAuthenticated());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_NoAccessTokenCookie_LeavesContextEmptyAndContinuesChain() throws Exception {
        request.setCookies(new Cookie("theme", "dark"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidToken_LeavesContextEmptyAndContinuesChain() throws Exception {
        String token = "invalid.token";
        request.setCookies(new Cookie("access_token", token));

        when(jwtService.extractUserId(token)).thenReturn(1L);
        when(jwtService.validateToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UserNotFoundInDb_LeavesContextEmptyAndContinuesChain() throws Exception {
        String token = "valid.token.deleted.user";
        request.setCookies(new Cookie("access_token", token));

        when(jwtService.extractUserId(token)).thenReturn(99L);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_JwtServiceThrowsException_CatchesSafelyAndContinuesChain() throws Exception {
        String token = "malformed.token";
        request.setCookies(new Cookie("access_token", token));

        when(jwtService.extractUserId(token)).thenThrow(new RuntimeException("Malformed JWT"));

        assertDoesNotThrow(() ->
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}