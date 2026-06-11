package com.redislab.refreshtoken.dto;

public record RefreshTokenVerifyResponse(
    String userId,
    boolean valid,
    long ttlSeconds,
    String message
) {
}
