package com.redislab.view.dto;

public record ViewResponse(
        String postId,
        String viewerId,
        boolean counted, // 이번 요청으로 카운트가 증가 되었는가?
        long viewCount, // 현재 조회수
        long duplicateBlockTtlSeconds, // 다시 조회수 증가가 가능해지기까지 남은 시간
        String message) {
}
