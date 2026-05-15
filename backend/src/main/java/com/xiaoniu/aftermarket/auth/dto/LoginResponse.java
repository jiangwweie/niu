package com.xiaoniu.aftermarket.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        AuthUserResponse user
) {
}
