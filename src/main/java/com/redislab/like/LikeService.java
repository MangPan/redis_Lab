package com.redislab.like;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.like.dto.LikeCountResponse;
import com.redislab.like.dto.LikeMemberResponse;
import com.redislab.like.dto.LikeRequest;
import com.redislab.like.dto.LikeResponse;
import com.redislab.like.dto.LikeStatusResponse;

import lombok.RequiredArgsConstructor;

/**
 * Redis(Set)를 활용한 게시글 좋아요 로직 처리 서비스 클래스
 * Key 포멧 - post:{postId}:likes -> Value: [userId1, userId2, ...] (Set 자료구조)
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 특정 게시글에 좋아요를 추가한다.
     *  
     * @param postId 게시글 ID
     * @param request 좋아요를 누른 유저 정보가 담긴 DTO
     * @return 좋아요 처리 결과 (현재 게시글의 좋아요 수, 성공 여부 메시지 등)
     */
    public LikeResponse like(String postId, LikeRequest request) {
        String normalizedPostId = normalize(postId);
        String normalizedUserId = normalize(request.userId());
        String key = buildLikeKey(normalizedPostId);

        // Redis SADD 명령어 실행: Set에 유저 ID 추가 (중복 허용 X)
        // 새로 추가성공시 1, 이미 존재했을시 0 반환
        Long addedCount = redisTemplate.opsForSet().add(key, normalizedUserId); // SADD(Set ADD)
        long likeCount = getLikeCountValue(key);

        // addedCount가 1 이상이면 이번 요청으로 처음 좋아요가 반영된 것임
        boolean newlyLiked = (addedCount != null) && (addedCount > 0);

        return new LikeResponse(
                normalizedPostId,
                normalizedUserId,
                true,
                likeCount,
                newlyLiked ? "좋아요가 추가되었습니다." : "이미 좋아요한 게시글입니다.");
    }

    /**
     * 특정 게시글의 좋아요를 취소한다.
     *  
     * @param postId 게시글 ID
     * @param request 좋아요를 취소할 유저 정보가 담긴 DTO
     * @return 좋아요 취소 처리 결과
     */
    public LikeResponse unlike(String postId, LikeRequest request) {
        String normalizedPostId = normalize(postId);
        String normalizedUserId = normalize(request.userId());
        String key = buildLikeKey(normalizedPostId);

        // Reids SREM 명령어 실행: Set에서 유저 ID 제거
        // 성공적으로 삭제되면 1, 삭제할 데이터가 없었으면 0 반환
        Long removedCount = redisTemplate.opsForSet().remove(key, normalizedUserId);
        long likeCount = getLikeCountValue(key);

        // removedCount가 1이상이면 실제로 좋아요가 취소된 것임
        boolean removed = removedCount != null && removedCount > 0;

        return new LikeResponse(
                normalizedPostId,
                normalizedUserId,
                false,
                likeCount,
                removed ? "좋아요가 취소되었습니다." : "좋아요 상태가 아니었습니다.");
    }

    /**
     * 특정 게시글의 총 좋아요 개수를 조회한다.
     * 
     * @param postId 게시글 ID
     * @return 게시글 ID와 좋아요 개수
     */
    public LikeCountResponse getLikeCount(String postId) {
        String normalizedPostId = normalize(postId);
        String key = buildLikeKey(normalizedPostId);

        return new LikeCountResponse(
                normalizedPostId,
                getLikeCountValue(key));
    }

    /**
     * 특정 유저가 해당 게시글에 좋아요를 눌렀는지 여부를 확인한다.
     * 
     * @param postId 게시글 ID
     * @param userId 유저 ID
     * @return 좋아요 여부 결과 정보
     */
    public LikeStatusResponse getMyLikeStatus(String postId, String userId) {
        String normalizedPostId = normalize(postId);
        String normalizedUserId = normalize(userId);
        String key = buildLikeKey(normalizedPostId);

        // Redis SISMEMBER 명령어 실행: 해당 유저가 Set의 회원인지 확인(O(1) 시간복잡도) # Set isMember
        Boolean liked = redisTemplate.opsForSet().isMember(key, normalizedUserId);

        return new LikeStatusResponse(
            normalizedPostId,
            normalizedUserId,
            Boolean.TRUE.equals(liked)
        );
    }


    /**
     * 특정 게시글에 좋아요를 누른 모든 유저 목록을 조회한다.
     * 
     * @param postId 게시글 ID
     * @return 유저 ID 목록 (Set 형태)
     */
    public LikeMemberResponse getLikeMembers(String postId){
        String normalizedPostId = normalize(postId);
        String key = buildLikeKey(normalizedPostId);

        // Redis SMEMBERS 명령어 실행: Set의 모든 element를 반환 # Set Members
        Set<String> userIds = redisTemplate.opsForSet().members(key);

        return new LikeMemberResponse(
            normalizedPostId,
            userIds != null ? userIds : Set.of() // null일 경우 빈 Set를 반환하여 NullPointerException 방지
        );
    }

    // ========================= 헬퍼 함수들 =========================

    /**
     * 게시글 ID를 기반으로 Redis Key를 생성한다.
     * ex: post:123:likes
     */
    private String buildLikeKey(String postId) {
        return "post:" + postId + ":likes";
    }

    /**
     * Redis SCARD 명령어를 통해 Set의 크기(좋아요 총합)을 가져온다.
     */
    private long getLikeCountValue(String key) {
        // Redis SCARD 명령어 실행: Set의 cardinality(크기) 반환
        Long size = redisTemplate.opsForSet().size(key);

        if (size == null) {
            return 0;
        }

        return size;
    }

    private String normalize(String value) {
        return value.trim();
    }

}
