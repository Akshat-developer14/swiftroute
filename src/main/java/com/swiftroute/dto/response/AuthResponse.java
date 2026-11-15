package com.swiftroute.dto.response;

public record AuthResponse(
        String message,
        UserResponse user
) {
    public static AuthResponse of(String message, UserResponse user) {
        return new AuthResponse(message, user);
    }
}
