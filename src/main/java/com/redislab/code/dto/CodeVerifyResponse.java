package com.redislab.code.dto;

public record CodeVerifyResponse(
    String email,
    boolean verified,
    String message
) {
}
