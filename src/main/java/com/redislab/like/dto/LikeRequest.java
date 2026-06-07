package com.redislab.like.dto;

import jakarta.validation.constraints.NotBlank;

public record LikeRequest(
    @NotBlank(message = "userId는 필수입니다.")
    String userId
) {
}
