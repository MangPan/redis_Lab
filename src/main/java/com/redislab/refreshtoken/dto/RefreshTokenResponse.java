package com.redislab.refreshtoken.dto;

public record RefreshTokenResponse(
    String userId,
    String refreshToken,
    long ttlSeconds,
    String redisKey,
    String message
) {
}
