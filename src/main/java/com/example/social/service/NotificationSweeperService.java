package com.example.social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationSweeperService {
    
    private final RedisGuardrailService redisGuardrailService;
    
    // Phase 3.2: CRON Sweeper - runs every 5 minutes
    // In production, this would run every 15 minutes, but for testing we use 5
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void sweepPendingNotifications() {
        log.info("🔄 Starting notification sweep...");
        
        List<String> userIds = redisGuardrailService.getAllUsersWithPendingNotifications();
        
        if (userIds.isEmpty()) {
            log.info("No pending notifications to process");
            return;
        }
        
        log.info("Found {} users with pending notifications", userIds.size());
        
        for (String userIdStr : userIds) {
            Long userId;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in Redis key: {}", userIdStr);
                continue;
            }
            
            List<Object> pendingMessages = redisGuardrailService.getAndClearPendingNotifications(userId);
            
            if (pendingMessages.isEmpty()) {
                continue;
            }
            
            // Summarize notifications
            int totalCount = pendingMessages.size();
            String firstBot = extractBotNameFromMessage(pendingMessages.get(0).toString());
            
            if (totalCount == 1) {
                log.info("📱 Summarized Push Notification to User {}: {}", userId, pendingMessages.get(0));
            } else {
                log.info("📱 Summarized Push Notification to User {}: {} and {} others interacted with your posts.",
                    userId, firstBot, totalCount - 1);
            }
        }
        
        log.info("✅ Notification sweep completed");
    }
    
    private String extractBotNameFromMessage(String message) {
        // Extract bot name from message like 'Bot "TestBot" replied to your post (123)'
        int start = message.indexOf("\"");
        int end = message.indexOf("\"", start + 1);
        if (start != -1 && end != -1) {
            return message.substring(start + 1, end);
        }
        return "A bot";
    }
}