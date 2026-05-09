package com.example.social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final RedisGuardrailService redisGuardrailService;
    
    public void handleBotReplyNotification(Long userId, Long postId, String botName) {
        // Phase 3.1: Check if user can receive immediate notification
        if (redisGuardrailService.canReceiveImmediateNotification(userId)) {
            // Send immediate notification
            log.info("🔔 IMMEDIATE: Bot \"{}\" replied to your post ({})", botName, postId);
            
            // Set cooldown
            redisGuardrailService.setNotificationCooldown(userId);
        } else {
            // Queue for batching
            String message = String.format("Bot \"%s\" replied to your post (%d)", botName, postId);
            redisGuardrailService.queuePendingNotification(userId, message);
            log.debug("📦 Queued notification for user {}: {}", userId, message);
        }
    }
}