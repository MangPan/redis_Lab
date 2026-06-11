package com.redislab.ratelimit.dto;

public record RateLimitStatusResponse(
    String userId,
    long currentCount,
    long limit,
    long ttlSeconds,
    String redisKey,
    String message
) {
}
