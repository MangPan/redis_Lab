package com.redislab.like.dto;

public record LikeStatusResponse(
        String postId,
        String userId,
        boolean liked) {
}
