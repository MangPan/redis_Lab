package com.redislab.health;

public record RedisHealthResponse(
    String status,
    String message
) {
}