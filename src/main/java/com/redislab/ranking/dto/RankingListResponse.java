package com.redislab.ranking.dto;

import java.util.List;

public record RankingListResponse(
    String rankingKey,
    long size,
    List<RankingItemResponse> items
) {
}