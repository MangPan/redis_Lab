package com.redislab.ranking.dto;

public record RankingStatusResponse(
    String postId,
    String member,
    Double score,
    Long rank,
    boolean exists
) {
}
