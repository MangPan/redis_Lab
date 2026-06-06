package com.redislab.code.dto;

public record CodeTtlResponse(
    String email,
    String redisKey,
    long ttlSeconds,
    String meaning
) {
}