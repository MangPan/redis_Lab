package com.redislab.search.dto;

import java.util.List;

public record SearchResponse(
    String userId,
    String keyword,
    List<String> recentSearches,
    String message
) {
    
}
