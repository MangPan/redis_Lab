package com.redislab.view;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.view.dto.ViewCountResponse;
import com.redislab.view.dto.ViewRequest;
import com.redislab.view.dto.ViewResponse;

@Service
public class ViewService {
    // 중복 조회 방지 시간
    private static final long DUPLICATE_BLOCK_SECONDS = 600;

    private final StringRedisTemplate redisTemplate;

    public ViewService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    /**
     * 게시글 조회수를 증가시킨다
     */
    public ViewResponse increaseView(String postId, ViewRequest request){
        String normalizedPostId = normalize(postId);
        String normalizedViewerId = normalize(request.viewerId());

        String viewedKey = buildViewedKey(normalizedPostId, normalizedViewerId);
        String viewCountKey = buildViewCountKey(normalizedPostId);

        // SETNX 연산을 통해 이 사용자가 이 게시글을 처음 조회했는지 확인한다.
        // SETNX = if key not exist set key value;
        Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(
            viewedKey, 
            "1",
            Duration.ofSeconds(DUPLICATE_BLOCK_SECONDS)
        );

        boolean counted = Boolean.TRUE.equals(isFirstView);

        // 처음 조회한 경우에만 Redis의 조회수 카운터를 1 증가시킨다.
        if(counted){
            redisTemplate.opsForValue().increment(viewCountKey);
        }

        long viewCount = getViewCountValue(viewCountKey);
        long ttl = getTtl(viewedKey);

        return new ViewResponse(
            normalizedPostId,
            normalizedViewerId,
            counted,
            viewCount,
            ttl,
            counted ? "조회수가 증가했습니다." : "중복 조회로 조회수가 증가하지 않았습니다."
        );
    }

    /**
     * 툭정 게시글의 현재 조회수를 조회한다.
     */
    public ViewCountResponse getViewCount(String postId){
        String normalizedPostId = normalize(postId);
        String viewCountKey = buildViewCountKey(normalizedPostId);

        return new ViewCountResponse(
            normalizedPostId,
            getViewCountValue(viewCountKey)
        );
    }

    /**
     * 특정 사용자의 중복 조회 방지 남은 시간(TTL)을 조회한다.
     */
    public ViewResponse getViewerTtl(String postId, String viewerId){
        String normalizedPostId = normalize(postId);
        String normalizedViewerId = normalize(viewerId);

        String viewedKey = buildViewedKey(normalizedPostId, normalizedViewerId);
        String viewCountKey = buildViewCountKey(normalizedPostId);

        long ttl = getTtl(viewedKey);

        return new ViewResponse(
            normalizedPostId,
            normalizedViewerId,
            false,
            getViewCountValue(viewCountKey),
            ttl,
            explainDuplicateState(ttl)
        );
    }

    /**
     * 특정 게시글의 조회수 데이터를 Redis에서 삭제한다.
     */
    public void resetViewCount(String postId){
        String normalizedPostId = normalize(postId);
        String viewCountKey = buildViewCountKey(normalizedPostId);

        redisTemplate.delete(viewCountKey);
    }

    /**
     * Redis Key 생성: 게시글 전체 조회수 카운트용
     */
    private String buildViewCountKey(String postId){
        return "post:" + postId + ":view-count";
    }

    /**
     * Redis key 생성: 유저별 중복 방지 확인용
     * ex) viewed:post:123viewer:user456
     */
    private String buildViewedKey(String postId, String viewerId){
        return "viewed:post:" + postId + ":viewer:" + viewerId;
    }

    // Redis에서 조회수 값을 가져오며, 데이터가 없을 경우 0을 반환한다.
    private long getViewCountValue(String viewCountKey){
        String value = redisTemplate.opsForValue().get(viewCountKey);

        if(value == null){
            return 0;
        }

        return Long.parseLong(value);
    }

    // Redis Key의 남은 만료 시간(TTL)울 초 단위로 반환한다.
    private long getTtl(String key){
        Long ttl = redisTemplate.getExpire(key);

        if(ttl == null){
            return -2;
        }

        return ttl; 
    }

    private String explainDuplicateState(long ttl){
        if(ttl > 0){
            return "중복 조회 방지 시간이 남아 있습니다.";
        }

        if(ttl == -1){
            return "중복 조회 방지 key는 있지만 만료 시간이 없습니다. 비정상 상태입니다.";
        }

        if(ttl == -2){
            return "중복 조회 방지 key가 없습니다. 다음 조회 시 조회수가 증가할 수 있습니다.";
        }

        return "알 수 없는 상태입니다.";
    }

    private String normalize(String value){
        return value.trim();
    }
}
