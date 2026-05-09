package com.example.social.service;

import com.example.social.dto.CommentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyTestService {
    
    private final CommentService commentService;
    private final RedisGuardrailService redisGuardrailService;
    
    /**
     * Test for Phase 4 - Race Condition Test
     * Simulates 200 bots trying to comment on a single post simultaneously
     */
    public ConcurrencyTestResult testBotCommentConcurrency(Long postId, int botIdStart, int numberOfBots) {
        log.info("🧪 Starting concurrency test: {} bots commenting on post {}", numberOfBots, postId);
        
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numberOfBots);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Fire all requests concurrently
        for (int i = 0; i < numberOfBots; i++) {
            final int botId = botIdStart + i;
            executor.submit(() -> {
                try {
                    CommentRequest request = CommentRequest.builder()
                        .content("Auto-generated test comment from Bot " + botId)
                        .authorId((long) botId)
                        .authorType("BOT")
                        .parentCommentId(null)
                        .build();
                    
                    commentService.addComment(postId, request);
                    successCount.incrementAndGet();
                    log.debug("Bot {} successfully commented", botId);
                } catch (RuntimeException e) {
                    failureCount.incrementAndGet();
                    log.debug("Bot {} failed: {}", botId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify Redis counter
        Long actualBotCount = redisGuardrailService.getBotReplyCount(postId);
        boolean testPassed = actualBotCount != null && actualBotCount <= 100 && actualBotCount == successCount.get();
        
        ConcurrencyTestResult result = ConcurrencyTestResult.builder()
            .totalRequests(numberOfBots)
            .successCount(successCount.get())
            .failureCount(failureCount.get())
            .redisBotCount(actualBotCount != null ? actualBotCount : 0)
            .durationMs(duration)
            .testPassed(testPassed)
            .build();
        
        log.info("📊 Concurrency test completed: {}", result);
        
        if (!testPassed) {
            log.error("❌ CONCURRENCY TEST FAILED! Redis count: {}, Success count: {}", 
                result.getRedisBotCount(), result.getSuccessCount());
        } else {
            log.info("✅ CONCURRENCY TEST PASSED! Exactly {} bot comments were allowed", result.getSuccessCount());
        }
        
        return result;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ConcurrencyTestResult {
        private int totalRequests;
        private int successCount;
        private int failureCount;
        private long redisBotCount;
        private long durationMs;
        private boolean testPassed;
    }
}