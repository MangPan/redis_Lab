package com.redislab.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
    @NotBlank(message = "keyword는 필수입니다.")
    @Size(max = 50, message = "keyword는 50자 이하로 입력해주세요.")
    String keyword
) {
}
