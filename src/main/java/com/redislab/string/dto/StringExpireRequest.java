package com.redislab.string.dto;

import jakarta.validation.constraints.Positive;

public record StringExpireRequest(
    @Positive(message = "ttlSeconds는 1 이상이어야 합니다.") 
    long ttlSeconds) {
}
