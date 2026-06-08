package com.redislab.ranking;

import org.springframework.web.bind.annotation.RestController;

import com.redislab.ranking.dto.RankingListResponse;
import com.redislab.ranking.dto.RankingStatusResponse;
import com.redislab.ranking.dto.ScoreRequest;
import com.redislab.ranking.dto.ScoreResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequiredArgsConstructor
public class RankingController {
    private final RankingService rankingService; 

    @PostMapping("/api/posts/{postId}/score")
    public ScoreResponse inreaseScore(
        @PathVariable String postId,
        @Valid @RequestBody ScoreRequest request 
    ) {
        return rankingService.increaseScore(postId, request);
    }

    @GetMapping("/api/rankings/posts")
    public RankingListResponse getRankings(
        @RequestParam(defaultValue = "10") long limit
    ) {
        return rankingService.getRankings(limit);
    }

    @GetMapping("/api/rankings/posts/{postId}")
    public RankingStatusResponse getRankingStatus(
        @PathVariable String postId
    ) {
        return rankingService.getRankingStatus(postId);
    }

    @DeleteMapping("/api/rankings/posts")
    public ResponseEntity<Void> clearRanking(){
        rankingService.clearRanking();

        return ResponseEntity.noContent().build();
    }
    
    
    
}
