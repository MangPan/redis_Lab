package com.redislab.refreshtoken;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.refreshtoken.dto.RefreshTokenCreateRequest;
import com.redislab.refreshtoken.dto.RefreshTokenResponse;
import com.redislab.refreshtoken.dto.RefreshTokenVerifyRequest;
import com.redislab.refreshtoken.dto.RefreshTokenVerifyResponse;

/**
 * Redis를 활용하여 Refresh Token의 발급, 검증, 만료 및 로테이션(재발급)을 관리하는 서비스
 */
@Service
public class RefreshTokenService {
    
    // Refresh Token 유효 기간: 7일 (초 단위)
    private static final long REFRESH_TOKEN_TTL_SECONDS = 60 * 60 * 24 * 7;

    // Redis Key 관리를 위한 접두사 (네임스페이스 분리용)
    private static final String KEY_PREFIX = "auth:refresh:user:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    /**
     * 사용자의 새로운 Refresh Token을 생성하고 Redis에 저장한다.
     * 기존에 토큰이 존재했다면 덮어쓰고 만료 시간을 초기화한다.
     * 
     * @param request 사용자 ID를 포함한 생성 요청 DTO
     * @return 생성된 토큰 정보 및 Redis 메타데이터를 포함한 응답 DTO
     */
    public RefreshTokenResponse createRefreshToken(RefreshTokenCreateRequest request){
        String userId = normalize(request.userId());
        String key = buildKey(userId);
        String refreshToken = generateRefreshToken();

        // Redis SET 명령어 수행 (Key, Value, TTL 설정)
        redisTemplate.opsForValue().set(
            key, 
            refreshToken,
            Duration.ofSeconds(REFRESH_TOKEN_TTL_SECONDS)
        );

        return new RefreshTokenResponse(
            userId,
            refreshToken,
            getTtl(key),
            key,
            "Refresh Token이 저장되었습니다."
        );
    }


    /**
     * 클라이언트가 제출한 Refresh Token이 Redis에 저장한 값과 일치하는지 검증한다.
     * 
     * @param request 사용자 ID와 검증할 토큰을 포함한 요청 DTO
     * @return 검증 결과 (성공 여부 및 메시지)를 포함한 응답 DTO
     */
    public RefreshTokenVerifyResponse verifyRefreshToken(RefreshTokenVerifyRequest request){
        String userId = normalize(request.userId());
        String key = buildKey(userId);

        // Redis GET 명령어 수행
        String savedToken = redisTemplate.opsForValue().get(key);
        long ttl = getTtl(key);

        // Redis에 해당 Key가 없거나 만료된 경우
        if(savedToken == null){
            return new RefreshTokenVerifyResponse(
                userId,
                false,
                ttl,
                "Refresh Token이 없거나 만료되었습니다."
            );
        }

        // 토큰은 존재하지만 클라이언트가 보낸 값과 일치하지 않는 경우
        if(!savedToken.equals(request.refreshToken())){
            return new RefreshTokenVerifyResponse(
                userId,
                false,
                ttl,
                "Refresh Token이 일치하지 않습니다."
            );
        }

        // 토큰이 유효하고 일치하는 경우
        return new RefreshTokenVerifyResponse(
            userId,
            true,
            ttl,
            "Refresh Token이 유효합니다."
        );
    }


    /**
     * 특정 사용자의 현재 저장된 Refresh Token의 정보를 조회한다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 토큰 존재 여부 및 토큰 값을 포함한 응답 DTO
     */
    public RefreshTokenResponse getRefreshToken(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildKey(normalizedUserId);
        String refreshToken = redisTemplate.opsForValue().get(key);

        return new RefreshTokenResponse(
            normalizedUserId,
            refreshToken,
            getTtl(key),
            key,
            refreshToken == null ? "Refresh Token이 없거나 만료되었습니다." : "Refresh Token이 존재합니다."
        );
    }


    /**
     * 로그아웃 또는 보안 취약점 감지 시, 사용자의 Refresh Token을 강제로 삭제한다.
     * 
     * @param userId 토큰을 삭제할 사용자ID
     */
    public void deleteRefreshToken(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildKey(normalizedUserId);

        // Redis DEL 명령어 수행
        redisTemplate.delete(key);
    }


    /**
     * [Token Rotation] 기존 토큰을 만료시키고 새로운 Refresh Token을 발급하여 보안성을 강화한다.
     * 
     * @param userId 토큰을 교체할 사용자 ID
     * @return 새로 발급된 토큰 정보를 포함한 응답 DTO
     */
    public RefreshTokenResponse rotateRefreshToken(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildKey(normalizedUserId);
        String newRefreshToken = generateRefreshToken();

        // 동일한 Key에 새로운 토큰과 TTL을 설정하여 덮어쓰기(Overwrite) 진행
        redisTemplate.opsForValue().set(
            key, 
            newRefreshToken,
            Duration.ofSeconds(REFRESH_TOKEN_TTL_SECONDS)
        );

        return new RefreshTokenResponse(
            normalizedUserId,
            newRefreshToken,
            getTtl(key),
            key,
            "Refresh Token이 교체되었습니다."
        );
    }


    /**
     * 고유한 무작위 문자열(UUID)을 생성하여 Refresh Token으로 반환한다.
     */
    private String generateRefreshToken(){
        return UUID.randomUUID().toString();
    }

    private String buildKey(String userId){
        return KEY_PREFIX + userId;
    }

    private long getTtl(String key){
        Long ttl = redisTemplate.getExpire(key);

        if(ttl == null){
            return -2;
        }

        return ttl; 
    }

    private String normalize(String value){
        return value.trim();
    }
}
