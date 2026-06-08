package com.redislab.ranking.dto;

public record RankingItemResponse(
    long rank,
    String postId,
    String member,
    double score
) {
}
