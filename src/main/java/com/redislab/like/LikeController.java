package com.redislab.like;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.like.dto.LikeCountResponse;
import com.redislab.like.dto.LikeMemberResponse;
import com.redislab.like.dto.LikeRequest;
import com.redislab.like.dto.LikeResponse;
import com.redislab.like.dto.LikeStatusResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public LikeResponse like(
            @PathVariable String postId,
            @Valid @RequestBody LikeRequest request) {
        return likeService.like(postId, request);
    }

    @DeleteMapping
    public LikeResponse unlike(
            @PathVariable String postId,
            @Valid @RequestBody LikeRequest request) {
        return likeService.unlike(postId, request);
    }

    @GetMapping("/count")
    public LikeCountResponse getLikeCount(
            @PathVariable String postId) {
        return likeService.getLikeCount(postId);
    }

    @GetMapping("/me")
    public LikeStatusResponse getMyLikeStatus(
            @PathVariable String postId,
            @RequestParam String userId) {
        return likeService.getMyLikeStatus(postId, userId);
    }

    @GetMapping
    public LikeMemberResponse getLikeMembers(@PathVariable String postId) {
        return likeService.getLikeMembers(postId);
    }
}
