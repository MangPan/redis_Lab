package com.redislab.like.dto;

import java.util.Set;

public record LikeMemberResponse(
    String postId,
    Set<String> userIds
) {
}
