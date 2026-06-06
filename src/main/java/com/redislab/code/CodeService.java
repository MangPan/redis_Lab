package com.redislab.code;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.code.dto.CodeCreateRequest;
import com.redislab.code.dto.CodeResponse;
import com.redislab.code.dto.CodeTtlResponse;
import com.redislab.code.dto.CodeVerifyRequest;
import com.redislab.code.dto.CodeVerifyResponse;

/**
 * 이메일 인증번호 발급 및 검증을 담당하는 서비스 클래스
 * Redis를 활용하여 인증번호의 라이프 사이클(생성, 조회, 검증, 만료)를 관리한다.
 */
@Service
public class CodeService {
    
    private static final long CODE_TTL_SECONDS = 300; // 인증번호 유효시간
    private static final String KEY_PREFIX = "auth:code:"; // Redis Key 네임스페이스 구분을 위한 접두사

    private final StringRedisTemplate redisTemplate; 
    private final SecureRandom secureRandom = new SecureRandom(); // 암호학적으로 안전한 난수 생성기

    public CodeService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }


    /**
     * 인증번호 생성 및 저장
     * 사용자의 이메일로 6자리 인증번호를 생성하고, Reids에 설정된 TTL동안 저장한다.
     */
    public CodeResponse createCode(CodeCreateRequest request){
        String email = normalizeEmail(request.email());
        String code = generateSixDigitCode();
        String key = buildKey(email);

        // Redis의 String 구조(opsForValue)를 사용하여 Key-Value 및 만료시간 저장
        redisTemplate.opsForValue()
            .set(
                key, 
                code,
                Duration.ofSeconds(CODE_TTL_SECONDS) // 5분 뒤 자동 삭제 설정
            );
        
        return new CodeResponse(
            email,
            code,
            CODE_TTL_SECONDS,
            key
        );
    }


    /**
     * 인증번호 검증
     * 사용자가 입력한 인증번호를 Redis에 저장된 값과 비교한다.
     * 검증 성공 시 재사용 방지를 위해 해당 인증번호를 Redis에서 즉시 삭제한다.
     */
    public CodeVerifyResponse verifyCode(CodeVerifyRequest request){
        String email = normalizeEmail(request.email());
        String key = buildKey(email);

        // Redis에서 해당 Key의 인증번호를 조회
        String savedCode = redisTemplate.opsForValue().get(key);

        // 인증번호가 없거나 이미 만료된 경우
        if(savedCode == null){
            return new CodeVerifyResponse(
                email,
                false,
                "인증번호가 없거나 만료되었습니다."
            );
        }

        // 번호가 일치하지 않는 경우
        if(!savedCode.equals(request.code())){
            return new CodeVerifyResponse(
                email,
                false,
                "인증번호가 일치하지 않습니다."
            );
        }

        // 위 두 경우를 제외하고선 인증 성공. 일회성 인증이므로 바로 삭제
        redisTemplate.delete(key);

        return new CodeVerifyResponse(
            email,
            true,
            "인증에 성공했습니다."
        ); 
    }

    /**
     * 인증번호 데이터 상세 조회
     * 이메일을 통해 Redis에 남아있는 인증번호화 남은 만료 시간(TTL)을 조회한다.
     * 원래라면 Email로 코드를 보내야하지만 학습을 위해 임시로..
     */
    public CodeResponse getCode(String email){
        String normalizedEmail = normalizeEmail(email);
        String key = buildKey(normalizedEmail);

        String code = redisTemplate.opsForValue().get(key); // code 조회
        Long ttl = redisTemplate.getExpire(key); // 남은 만료 시간 조회

        return new CodeResponse(
            normalizedEmail,
            code,
            ttl != null ? ttl : -2, // ttl이 null이면 데이터가 없는 상태(-2)로 취급
            key
        );
    }

    /**
     * 인증번호의 남은 시간(TTL)만 조회
     * 디버깅용 혹은 프론트엔드 타이머 표시를 위해 해당 Key의 남은 수명을 확인한다.
     */
    public CodeTtlResponse getTtl(String email){
        String noramlizedEmail = normalizeEmail(email);
        String key = buildKey(noramlizedEmail);

        Long ttl = redisTemplate.getExpire(key);

        // ttl 결과를 가져오지 못했을 경우
        if(ttl == null){
            return new CodeTtlResponse(
                noramlizedEmail,
                key,
                -2,
                "TTL 정보를 가져올 수 없습니다."
            );
        }

        return new CodeTtlResponse(
            noramlizedEmail,
            key,
            ttl,
            explainTtl(ttl)
        );
    }


    /**
     * 인증번호 수동 삭제
     * 필요한 경우 특정 이메일의 인증번호 데이터를 Redis에서 즉시 파기한다.
     */
    public void deleteCode(String email){
        String normalizedEmail = normalizeEmail(email);
        String key = buildKey(normalizedEmail);

        redisTemplate.delete(key);
    }

    // ========================= 내부 헬퍼 메소드 =========================

    /**
     * 6자리 숫자로 구성된 무작위 인증번호 생성
     */
    private String generateSixDigitCode(){
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    /**
     * Redis 표준 네임스페이스 규칙(콜론:)에 따라 Key 이름 생성
     * ex) auth:code:text@example.com
     */
    private String buildKey(String email){
        return KEY_PREFIX + email;
    }

    /**
     * 이메일 문자열 정규화
     */
    private String normalizeEmail(String email){
        return email.trim().toLowerCase();
    }

    /**
     * Redis getExpire() 메서드가 반환하는 TTL 코드를 메시지로 해석
     * Redis 스펙 공식 기준:
     *  -1 : Key는 존재하나, 만료 시간이 지정되지 않은 상태(무한 저장)
     *  -2 : Key가 존재하지 않거나, 만료되어 이미 사라진 상태
     */
    private String explainTtl(long ttl){
        if(ttl > 0){
            return "인증번호가 존재하며 만료 시간이 설정되어 있습니다.";
        }

        if(ttl == -1){
            return "인증번호는 존재하지만 만료 시간이 없습니다. 현재 기능에서는 비정상 상태입니다.";
        }

        if(ttl == -2){
            return "인증번호가 존재하지 않거나 만료되었습니다.";
        }

        return "알 수 없는 TTL 상태입니다.";
    }
}
