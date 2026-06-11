package com.redislab.ratelimit;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.ratelimit.dto.LimitedActionRequest;
import com.redislab.ratelimit.dto.RateLimitResponse;
import com.redislab.ratelimit.dto.RateLimitStatusResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/limited-actions")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @PostMapping    
    public RateLimitResponse tryAction(
        @Valid @RequestBody LimitedActionRequest request
    ) {
        return rateLimitService.tryAction(request);
    }

    @GetMapping("/{userId}")
    public RateLimitStatusResponse getStatus(@PathVariable String userId) {
        return rateLimitService.getStatus(userId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> reset(@PathVariable String userId){
        rateLimitService.reset(userId);

        return ResponseEntity.noContent().build();
    }
}
