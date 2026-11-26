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
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)){
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.name().trim(),
                email,
                encodedPassword
        );

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public AuthTokensDto login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserResponse userResponse = userMapper.toResponse(user);
        return AuthTokensDto.builder()
                .accessToken(accessToken)
                .accessTokenExpirySeconds(jwtService.getAccessTokenExpirationSeconds())
                .refreshToken(refreshToken)
                .refreshTokenExpirySeconds(jwtService.getRefreshTokenExpirationSeconds())
                .user(userResponse)
                .build();
    }

    public String refreshAccessToken (String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token is missing");
        }

        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        Long userId = jwtService.extractUserId(refreshToken);

        if (userId == null) {
            throw new InvalidCredentialsException("Invalid token payload");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return jwtService.generateAccessToken(user);
    }
}
