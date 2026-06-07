package com.redislab.view.dto;

import jakarta.validation.constraints.NotBlank;

public record ViewRequest(
    @NotBlank(message = "viewerId는 필수입니다.")
    String viewerId
) {
}