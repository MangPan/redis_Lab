package com.redislab.search.dto;

import java.util.List;

public record RecentSearchesResponse(
    String userId,
    List<String> recentSearches,
    long size
) {
}