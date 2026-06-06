package com.redislab.string;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.string.dto.StringExpireRequest;
import com.redislab.string.dto.StringSetRequest;
import com.redislab.string.dto.StringSetWithTtlRequest;
import com.redislab.string.dto.StringValueResponse;
import com.redislab.string.dto.TtlResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StringService {
    
    private final StringRedisTemplate redisTemplate;

    public StringValueResponse setValue(StringSetRequest request){
        redisTemplate.opsForValue().set(request.key(), request.value());
        
        return new StringValueResponse(
            request.key(),
            request.value()
        );
    }

    public StringValueResponse setValueWithTtl(StringSetWithTtlRequest request){
        redisTemplate.opsForValue().set(
            request.key(), 
            request.value(), 
            Duration.ofSeconds(request.ttlSeconds())
        );

        return new StringValueResponse(
            request.key(),
            request.value()
        );
    }

    public StringValueResponse getValue(String key){
        String value = redisTemplate.opsForValue().get(key);

        return new StringValueResponse(
            key,
            value
        );
    }

    public void deleteValue(String key){
        redisTemplate.delete(key);
    }

    public TtlResponse getTtl(String key){
        Long ttl = redisTemplate.getExpire(key);

        if(ttl == null){
            return new TtlResponse(key, -2, "TTL 정보를 가져올 수 없습니다.");
        }

        return new TtlResponse(
            key,
            ttl,
            explainTtl(ttl)
        );
    }

    public TtlResponse expire(String key, StringExpireRequest request){
        redisTemplate.expire(key, Duration.ofSeconds(request.ttlSeconds()));

        Long ttl = redisTemplate.getExpire(key);

        if(ttl == null){
            return new TtlResponse(key, -2, "TTL 정보를 가져올 수 없습니다.");
        }

        return new TtlResponse(
            key,
            ttl,
            explainTtl(ttl)
        );
    }


    private String explainTtl(long ttl){
        if(ttl > 0){
            return "key가 존재하며 만료 시간이 설정되어 있습니다.";
        }

        if(ttl == -1){
            return "key가 존재하지만 만료 시간이 없습니다.";
        }

        if(ttl == -2){
            return "key가 존재하지 않습니다.";
        }

        return "알 수 없는 TTL 상태입니다.";
    }
}
