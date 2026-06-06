package com.redislab.health;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisHealthService {
    
    private final StringRedisTemplate redisTemplate;

    public RedisHealthResponse checkRedisConnection(){
        String pong = redisTemplate.getConnectionFactory()
            .getConnection()
            .ping();

        if("PONG".equalsIgnoreCase(pong)){
            return new RedisHealthResponse(
                "UP",
                "Redis connection successful"
            );
        }

        return new RedisHealthResponse(
            "DOWN",
            "Redis connection failed"
        );
    }
}
