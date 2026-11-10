package com.swiftroute.service;

import com.swiftroute.dto.request.RegisterRequest;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.entity.User;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
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
}
