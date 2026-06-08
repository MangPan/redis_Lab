package com.redislab.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.redislab.ranking.dto.RankingItemResponse;
import com.redislab.ranking.dto.RankingListResponse;
import com.redislab.ranking.dto.RankingStatusResponse;
import com.redislab.ranking.dto.ScoreRequest;
import com.redislab.ranking.dto.ScoreResponse;

import lombok.RequiredArgsConstructor;

/**
 * Redis Sorted Set(ZSET)을 활용한 게시글 랭킹 시스템 서비스
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    // Redis에 랭킹 데이터를 저장할 Key
    private static final String RANKING_KEY = "ranking:posts";

    private final StringRedisTemplate redisTemplate;

    /**
     * 특정 게시글의 점수를 누적하고, 반영된 점수와 현재 순위를 반환한다.
     * 
     * @param postId  게시글 ID
     * @param request 추가할 점수 정보가 담긴 DTO
     * @return 점수 누적 결과 및 랭킹 정보 (ScoreResponse)
     */
    public ScoreResponse increaseScore(String postId, ScoreRequest request) {
        String normalizedPostId = normalize(postId);
        String member = buildMember(normalizedPostId);

        // ZINCRBY: 해당 member의 점수를 지정된 만큼 증가시킨다. (반환값: 증가 완료된 총점)
        Double totalScore = redisTemplate.opsForZSet()
                .incrementScore(
                        RANKING_KEY,
                        member,
                        request.score());

        // ZREVRANK: 내림차순(높은 점수 순) 기준으로 해당 member의 순위를 조회한다. (0부터 시작)
        Long zeroBasedRank = redisTemplate.opsForZSet()
                .reverseRank(
                        RANKING_KEY,
                        member);

        return new ScoreResponse(
                normalizedPostId,
                member,
                request.score(),
                totalScore != null ? totalScore : 0.0,
                toDisplayRank(zeroBasedRank), // 1부터 시작하도록 수정
                "점수가 반영되었습니다.");
    }

    /**
     * 실시간 인기 랭킹 목록을 조회한다. (상위 N개)
     * 
     * @param limit 조회할 최대 게시글 수
     * @return 랭킹 목록 정보 (RankingListResponse)
     */
    public RankingListResponse getRankings(long limit) {
        long safeLimit = normalizeLimit(limit);

        // ZREVRANGEBYSCORE: 내림차순 정렬 기준 0번째부터 (safeLimit - 1)번째까지의 member와 score를 함께
        // 조회한다.
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(
                        RANKING_KEY,
                        0,
                        safeLimit - 1);

        List<RankingItemResponse> items = new ArrayList<>();

        if (tuples != null) {
            long rank = 1; // 랭킹 목록 내부에서 순차적으로 부여할 순위

            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String member = tuple.getValue();
                Double score = tuple.getScore();

                items.add(new RankingItemResponse(
                        rank,
                        extractPostId(member),
                        member,
                        score != null ? score : 0.0));

                rank++; // 다음 아이템 순위 증가
            }
        }

        return new RankingListResponse(
                RANKING_KEY,
                getRankingSize(), // 전체 랭킹에 등록된 게시글 수
                items);
    }

    /**
     * 특정 게시글의 현재 랭킹 상태(점수 및 순위)를 조회한다.
     * 
     * @param postId 게시글ID
     * @return 게시글의 랭킹 상태 정보
     */
    public RankingStatusResponse getRankingStatus(String postId) {
        String normalizedPostId = normalize(postId);
        String member = buildMember(normalizedPostId);

        // ZSCORE : 해당 member의 현재 점수 조회 (랭킹에 없으면 null)
        Double score = redisTemplate.opsForZSet()
                .score(RANKING_KEY, member);

        // ZREVRANK : 내림차순 기준 해당 member의 현재 순위 조회 (랭킹에 없으면 null 반환)
        Long zeroBasedRank = redisTemplate.opsForZSet()
                .reverseRank(RANKING_KEY, member);

        return new RankingStatusResponse(
                normalizedPostId,
                member,
                score,
                toDisplayRank(zeroBasedRank), // 1부터 시작
                score != null // 점수 존재 여부로 랭킹 등록 상태 확인
        );
    }

    /**
     * 전체 랭킹 데이터를 초기화한다.
     */
    public void clearRanking() {
        redisTemplate.delete(RANKING_KEY);
    }

    /**
     * ZCARD: 현재 랭킹(Sorted Set)에 등록된 전체 member(게시글)의 수를 반환한다.
     */
    private long getRankingSize() {
        Long size = redisTemplate.opsForZSet().size(RANKING_KEY);
        return size != null ? size : 0;
    }

    private String buildMember(String postId) {
        return "post:" + postId;
    }

    private String extractPostId(String member) {
        if (member == null) {
            return null;
        }
        if (member.startsWith("post:")) {
            return member.substring("post:".length());
        }

        return member;
    }

    private Long toDisplayRank(Long zeroBasedRank) {
        if (zeroBasedRank == null) {
            return null;
        }
        return zeroBasedRank + 1;
    }

    private long normalizeLimit(long limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }

    private String normalize(String value) {
        return value.trim();
    }
}
