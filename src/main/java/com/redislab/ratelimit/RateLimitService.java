package com.redislab.ratelimit;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.ratelimit.dto.*;

/**
 * Redis INCR, EXPIRE 응용
 * 고정 윈도우(Fixed Window) 방식의 처리량 제한(Rate Limiting)을 구현
 */
@Service
public class RateLimitService {

    // 윈도우당 최대 허용 요청 횟수
    private static final long LIMIT = 10;

    // 처리량 제한 기준 시간 
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    /**
     * 사용자의 요청을 처리할 수 있는지 확인하고, 카운터를 증가시킨다.
     * 
     * @param request 사용자 ID를 포함한 요청 객체
     * @return 요청 허용 여부, 현재 카운트, 잔여 시간(TTL) 등을 포함한 결과 객체
     */
    public RateLimitResponse tryAction(LimitedActionRequest request){
        String userId = normalize(request.userId());
        String key = buildKey(userId);

        // Redis의 INCR 명령 수행: 값을 1 증가시키고 증가된 결과값을 반환 (Key가 없으면 1로 생성됨)
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if(currentCount == null){
            currentCount = 0L;
        }
        
        // 카운트가 1이라는 것은 이번 윈도우(60초 제한 내에서) 첫 번째 요청임을 의미한다.
        if(currentCount == 1){
            // 첫 요청 시점에 60초 만료 시간(TTL) 설정 -> 60초 뒤 자동 초기화
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        long ttl = getTtl(key);
        // 현재 카운트가 제한값 이하인지 확인하여 허용 여부 결정
        boolean allowed = currentCount <= LIMIT;

        return new RateLimitResponse(
            userId,
            allowed,
            currentCount,
            LIMIT,
            ttl,
            allowed ? "요청이 허용되었습니다." : "요청 한도를 초과했습니다."
        );
    }

    /**
     * 특정 사용자의 현재 처리량 제한 상태(카운트, 잔여 시간 등)을 조회한다.
     * (카운트를 증가시키지 않고 상태만 모니터링할 때 사용)
     * 
     * @param userId userId 사용자 ID
     * @return 현재 제한 상태 정보를 담은 응답 객체
     */
    public RateLimitStatusResponse getStatus(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildKey(normalizedUserId);

        long currentCount = getCurrentCount(key);
        long ttl = getTtl(key);

        return new RateLimitStatusResponse(
            normalizedUserId,
            currentCount,
            LIMIT,
            ttl,
            key,
            explainStatus(currentCount, ttl)
        );
    }

    /**
     * 특정 사용자의 처리량 제한을 강제로 초기화(차단 해제)한다.
     * 
     * @param userId
     */
    public void reset(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildKey(normalizedUserId);

        redisTemplate.delete(key); // 걍 Redis에서 키 날림
    }


    /**
     * Redis에서 저장할 처리량 제한용 Key를 생성한다.
     * 형식 : rate-limit:user:{userId}
     */
    private String buildKey(String userId){
        return "rate-limit:user:" + userId;
    }

    /**
     * Redis에서 현재 요청 카운트 값을 안전하게 읽어온다.
     */
    private long getCurrentCount(String key){
        String value = redisTemplate.opsForValue().get(key);

        if(value == null){ // 데이터가 없으면(Redis에 키가 없으면) 요청 횟수가 0인 상태
            return 0;
        }

        return Long.parseLong(value);
    }

    /**
     * Redis에서 해당 Key의 남은 만료 시간(TTL, 초 단위)를 조회한다.
     * @return 남은 시간(초). 만약 Key가 없으면 -2, TTL이 설정되어 있지 않으면 -1 반환
     */
    private long getTtl(String key){
        Long ttl = redisTemplate.getExpire(key);
        
        if(ttl == null){
            return -2; // Redis 전송 오류 또는 만료 등으로 확인이 불가능할 때 기본값 처리
        }

        return ttl; 
    }

    private String explainStatus(long currentCount, long ttl){
        if(ttl == -2){
            return "현재 제한 상태가 없습니다. 다음 요청부터 새 윈도우가 시작됩니다.";
        }

        if(ttl == -1){
            return "key는 존재하지만 TTL이 없습니다. 비정상 상태입니다.";
        }

        if(currentCount > LIMIT){
            return "요청 한도를 초과한 상태입니다.";
        }

        return "요청 가능한 상태입니다.";
    }

    private String normalize(String value){
        return value.trim();
    }


}
