package com.redislab.string.dto;

public record TtlResponse(
    String key,
    long ttlSeconds,
    String meaning
) {
}