package com.redislab.like.dto;

public record LikeCountResponse(
    String postId,
    long likeCount
) {
}
