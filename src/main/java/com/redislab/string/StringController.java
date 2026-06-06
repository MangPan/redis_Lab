package com.redislab.string;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.string.dto.StringExpireRequest;
import com.redislab.string.dto.StringSetRequest;
import com.redislab.string.dto.StringSetWithTtlRequest;
import com.redislab.string.dto.StringValueResponse;
import com.redislab.string.dto.TtlResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/strings")
@RequiredArgsConstructor
public class StringController {

    private final StringService stringService;

    @PostMapping
    public StringValueResponse setValue(
        @Valid @RequestBody StringSetRequest request
    ){
        return stringService.setValue(request);
    }

    @PostMapping("/ttl")
    public StringValueResponse setValueWithTtl(
        @Valid @RequestBody StringSetWithTtlRequest request
    ) {
        return stringService.setValueWithTtl(request);
    }

    @GetMapping("/{key}")
    public StringValueResponse getValue(@PathVariable String key) {
        return stringService.getValue(key);
    }
    
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteValue(@PathVariable String key){
        stringService.deleteValue(key);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key}/ttl")
    public TtlResponse getTtl(@PathVariable String key) {
        return stringService.getTtl(key);
    }

    @PostMapping("/{key}/expire")
    public TtlResponse expire(
        @PathVariable String key,
        @Valid @RequestBody StringExpireRequest request
    ) {
        return stringService.expire(key, request);
    }
    
    
    
    
}
