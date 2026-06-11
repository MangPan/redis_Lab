package com.redislab.refreshtoken;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.refreshtoken.dto.RefreshTokenCreateRequest;
import com.redislab.refreshtoken.dto.RefreshTokenResponse;
import com.redislab.refreshtoken.dto.RefreshTokenVerifyRequest;
import com.redislab.refreshtoken.dto.RefreshTokenVerifyResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/auth/refresh-tokens")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping    
    public RefreshTokenResponse createRefreshToken(
        @Valid @RequestBody RefreshTokenCreateRequest request
    ) {
        return refreshTokenService.createRefreshToken(request);
    }

    @PostMapping("/verify")
    public RefreshTokenVerifyResponse verifyRefreshToken(
        @Valid @RequestBody RefreshTokenVerifyRequest request
    ) {
        return refreshTokenService.verifyRefreshToken(request);
    }

    @GetMapping("/{userId}")
    public RefreshTokenResponse getRefreshToken(
        @PathVariable String userId
    ) {
        return refreshTokenService.getRefreshToken(userId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteRefreshToken(
        @PathVariable String userId
    ){
        refreshTokenService.deleteRefreshToken(userId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/rotate")
    public RefreshTokenResponse rotateRefreshToken(
        @PathVariable String userId
    ) {
        return refreshTokenService.rotateRefreshToken(userId);
    }
    
    
    
    
    
}
