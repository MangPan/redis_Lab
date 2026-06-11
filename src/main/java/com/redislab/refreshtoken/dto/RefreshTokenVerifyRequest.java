package com.redislab.refreshtoken.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenVerifyRequest(
    @NotBlank(message = "userId는 필수입니다.")
    String userId,

    @NotBlank(message = "refreshToken은 필수입니다.")
    String refreshToken
) {
}
