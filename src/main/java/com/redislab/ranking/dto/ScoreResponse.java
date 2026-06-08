package com.redislab.ranking.dto;

public record ScoreResponse(
    String postId,
    String member,
    double addedScore,
    double totalScore,
    Long rank,
    String message
) {
}
