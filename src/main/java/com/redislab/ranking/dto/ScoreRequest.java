package com.redislab.ranking.dto;

import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
    @NotNull(message = "score는 필수입니다.")
    Double score
) {
}
