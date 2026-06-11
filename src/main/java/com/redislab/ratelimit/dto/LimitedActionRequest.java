package com.redislab.ratelimit.dto;

import jakarta.validation.constraints.NotBlank;

public record LimitedActionRequest(
    @NotBlank(message = "userId는 필수입니다.")
    String userId
) {
}
