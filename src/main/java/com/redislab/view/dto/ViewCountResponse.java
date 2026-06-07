package com.redislab.view.dto;

public record ViewCountResponse(
    String postId,
    long viewCount
) {
}
