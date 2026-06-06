package com.redislab.string.dto;

import jakarta.validation.constraints.NotBlank;

public record StringSetRequest(
    @NotBlank(message = "key는 필수입니다.") 
    String key,

    @NotBlank(message = "value는 필수입니다.") 
    String value
) {
}