package com.redislab.search;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.search.dto.RecentSearchesResponse;
import com.redislab.search.dto.SearchRequest;
import com.redislab.search.dto.SearchResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/users/{userId}/searches")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public SearchResponse addSearch(
            @PathVariable String userId,
            @Valid @RequestBody SearchRequest request) {
        return searchService.addSearch(userId, request);
    }

    @GetMapping
    public RecentSearchesResponse getRecentSearches(
        @PathVariable String userId
    ) {
        return searchService.getRecentSearches(userId);
    }

    @DeleteMapping("/{keyword}")
    public SearchResponse deleteKeyword(
            @PathVariable String userId,
            @PathVariable String keyword) {
        return searchService.deleteKeyword(userId, keyword);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearRecentSearches(
            @PathVariable String userId) {
        searchService.cleanRecentSearches(userId);
        return ResponseEntity.noContent().build();
    }

}
