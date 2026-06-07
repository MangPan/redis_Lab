package com.redislab.view;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.view.dto.ViewCountResponse;
import com.redislab.view.dto.ViewRequest;
import com.redislab.view.dto.ViewResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts/{postId}/views")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @PostMapping
    public ViewResponse increaseView(
            @PathVariable String postId,
            @Valid @RequestBody ViewRequest request) {
        return viewService.increaseView(postId, request);
    }

    @GetMapping
    public ViewCountResponse getViewCount(@PathVariable String postId) {
        return viewService.getViewCount(postId);
    }

    @GetMapping("/viewers/{viewerId}/ttl")
    public ViewResponse getViewerTtl(
            @PathVariable String postId,
            @PathVariable String viewerId) {
        return viewService.getViewerTtl(postId, viewerId);
    }

    @DeleteMapping
    public ResponseEntity<Void> resetViewCount(@PathVariable String postId){
        viewService.resetViewCount(postId);

        return ResponseEntity.noContent().build();
    }
}
