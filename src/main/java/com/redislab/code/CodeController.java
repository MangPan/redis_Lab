package com.redislab.code;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redislab.code.dto.CodeCreateRequest;
import com.redislab.code.dto.CodeResponse;
import com.redislab.code.dto.CodeTtlResponse;
import com.redislab.code.dto.CodeVerifyRequest;
import com.redislab.code.dto.CodeVerifyResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    @PostMapping
    public CodeResponse createCode(
            @Valid @RequestBody CodeCreateRequest request) {
        return codeService.createCode(request);
    }

    @PostMapping("/verify")
    public CodeVerifyResponse verifyCode(
        @Valid @RequestBody CodeVerifyRequest request
    ) {
        return codeService.verifyCode(request);
    }

    @GetMapping("/{email}")
    public CodeResponse getCode(@PathVariable String email) {
        return codeService.getCode(email);
    }

    @GetMapping("/{email}/ttl")
    public CodeTtlResponse getTtl(@PathVariable String email) {
        return codeService.getTtl(email);
    }
    

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteCode(@PathVariable String email){
        codeService.deleteCode(email);

        return ResponseEntity.noContent().build();
    }

}
