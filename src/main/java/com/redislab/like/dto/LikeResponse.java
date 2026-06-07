package com.redislab.like.dto;

public record LikeResponse(
    String postId,
    String userId,
    boolean liked,
    long likeCount,
    String message
) {
}
