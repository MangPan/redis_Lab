package com.redislab.ratelimit.dto;

public record RateLimitResponse(
    String userId,
    boolean allowed,
    long currentCount,
    long limit,
    long ttlSeconds,
    String message
) {
}
