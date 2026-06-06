package com.redislab.string.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StringSetWithTtlRequest(
    @NotBlank(message = "key는 필수입니다.")
    String key,

    @NotBlank(message = "value는 필수입니다.")
    String value,

    @Positive(message = "ttlSeconds는 1 이상이어야 합니다.")
    long ttlSeconds
) {
}