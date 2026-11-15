package com.swiftroute.dto.helper;

import com.swiftroute.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AuthTokensDto {
    private final String accessToken;
    private final Long accessTokenExpirySeconds;
    private final String refreshToken;
    private final Long refreshTokenExpirySeconds;
    private final UserResponse user;
}
