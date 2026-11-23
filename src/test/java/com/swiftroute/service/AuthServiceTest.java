package com.swiftroute.service;

import com.swiftroute.dto.helper.AuthTokensDto;
import com.swiftroute.dto.request.LoginRequest;
import com.swiftroute.dto.request.RegisterRequest;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.entity.User;
import com.swiftroute.exception.InvalidCredentialsException;
import com.swiftroute.exception.UserAlreadyExistsException;
import com.swiftroute.mapper.UserMapper;
import com.swiftroute.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(" Akshat Sharma", "Akshat@email.com ", "rawPassword");
        Instant now = Instant.parse("2026-11-22T10:00:00Z");
        UserResponse expectedResponse = new UserResponse(1L, "Akshat", "akshat@email.com", now);

        when(userRepository.existsByEmail("akshat@email.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse result = authService.register(request);

        assertEquals(expectedResponse, result);
        verify(userRepository).save(argThat(user ->
                user.getId().equals(1L) &&
                user.getName().equals("Akshat Sharma") &&
                user.getEmail().equals("akshat@email.com") &&
                user.getPasswordHash().equals("hashedPassword")
        ));
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionInRegisterWhenEmailIsTaken() {
        RegisterRequest request = new RegisterRequest("Akshat", "akshat@example.com", "rawPassword");
        when(userRepository.existsByEmail("akshat@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldLoginSuccessfullyAndReturnAccessAndRefreshToken() {
        LoginRequest request = new LoginRequest(" Akshat@email.com", "rawPassword");
        User user = new User("Akshat Sharma", "akshat@email.com", "hashedPassword");
        ReflectionTestUtils.setField(user, "id" , 1L);

        Instant now = Instant.parse("2026-11-22T10:00:00Z");
        UserResponse expectedUserResponse = new UserResponse(1L, "Akshat Sharma", "akshat@example.com", now);

        when(userRepository.findByEmail("akshat@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(604_800L);
        when(userMapper.toResponse(user)).thenReturn(expectedUserResponse);

        AuthTokensDto result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals(900L, result.getAccessTokenExpirySeconds());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(604_800L, result.getRefreshTokenExpirySeconds());
        assertEquals(expectedUserResponse, result.getUser());
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionInLoginWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("akshat@email.com", "rawPassword");
        when(userRepository.findByEmail("akshat@email.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionInLoginWhenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("akshat@email.com", "wrongPassword");
        User user = new User("Akshat Sharma", "akshat@email.com", "hashedPassword");
        when(userRepository.findByEmail("akshat@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRefreshAccessTokenSuccessfully() {
        String refreshToken = "my-jwt-refresh-token";
        User user = new User("Akshat Sharma", "akshat@email.com", "hashedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-jwt-access-token");

        String newAccessToken = authService.refreshAccessToken(refreshToken);

        assertEquals("new-jwt-access-token", newAccessToken);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldThrowInvalidCredentialsExceptionWhenRefreshTokenIsEmptyOrNull(String refreshToken) {
        assertThrows(InvalidCredentialsException.class, () -> authService.refreshAccessToken(refreshToken));
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenRefreshTokenIsInvalid() {
        String refreshToken = "my-jwt-invalid-refresh-token";
        when(jwtService.validateToken(refreshToken)).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> authService.refreshAccessToken(refreshToken));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenRefreshTokenUserIdIsNull() {
        String refreshToken = "my-jwt-refresh-token-with-null-userid";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(null);

        assertThrows(InvalidCredentialsException.class, () -> authService.refreshAccessToken(refreshToken));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenRefreshTokenUserIdNotFound() {
        String refreshToken = "my-jwt-refresh-token-with-null-userid";

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.refreshAccessToken(refreshToken));
        verify(jwtService, never()).generateAccessToken(any());
    }
}
