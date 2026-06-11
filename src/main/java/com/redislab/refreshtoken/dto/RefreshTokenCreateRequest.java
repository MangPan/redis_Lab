package com.redislab.refreshtoken.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCreateRequest(
    @NotBlank(message = "userId는 필수입니다.")
    String userId
) {
}
