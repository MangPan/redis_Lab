package com.redislab.health;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
public class RedisHealthController {

    private final RedisHealthService redisHealthService;

    @GetMapping("/health/redis")
    public RedisHealthResponse checkRedis() {
        return redisHealthService.checkRedisConnection();
    }
    
}
