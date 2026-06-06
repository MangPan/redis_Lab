package com.redislab.code.dto;

public record CodeResponse(
    String email,
    String code,
    long ttlSeconds,
    String redisKey
) {
    
}
