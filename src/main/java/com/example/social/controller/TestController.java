package com.example.social.controller;

import com.example.social.dto.ApiResponse;
import com.example.social.service.ConcurrencyTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final ConcurrencyTestService concurrencyTestService;
    
    @PostMapping("/concurrency/{postId}")
    public ResponseEntity<ApiResponse<ConcurrencyTestService.ConcurrencyTestResult>> testConcurrency(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "200") int numberOfBots,
            @RequestParam(defaultValue = "1000") int botIdStart) {
        
        ConcurrencyTestService.ConcurrencyTestResult result = 
            concurrencyTestService.testBotCommentConcurrency(postId, botIdStart, numberOfBots);
        
        if (result.isTestPassed()) {
            return ResponseEntity.ok(ApiResponse.success("Concurrency test passed!", result));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(
                String.format("Concurrency test failed! Expected <=100, got %d", result.getRedisBotCount())));
        }
    }
}