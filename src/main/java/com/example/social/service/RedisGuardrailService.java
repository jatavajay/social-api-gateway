package com.example.social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisGuardrailService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> incrWithLimitScript;
    
    private static final String VIRALITY_SCORE_PREFIX = "post:";
    private static final String VIRALITY_SCORE_SUFFIX = ":virality_score";
    private static final String BOT_COUNT_PREFIX = "post:";
    private static final String BOT_COUNT_SUFFIX = ":bot_count";
    private static final String COOLDOWN_PREFIX = "cooldown:bot_";
    private static final String NOTIF_COOLDOWN_PREFIX = "notif_cooldown:user_";
    private static final String PENDING_NOTIF_PREFIX = "user:";
    private static final String PENDING_NOTIF_SUFFIX = ":pending_notifs";
    
    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_COMMENT_DEPTH = 20;
    private static final long COOLDOWN_SECONDS = 600; // 10 minutes
    private static final long NOTIF_COOLDOWN_SECONDS = 900; // 15 minutes
    
    // Phase 2.1: Increment Virality Score
    public void incrementViralityScore(Long postId, InteractionType type) {
        String key = VIRALITY_SCORE_PREFIX + postId + VIRALITY_SCORE_SUFFIX;
        int points = type.getPoints();
        
        redisTemplate.opsForValue().increment(key, points);
        log.debug("Incremented virality score for post {} by {} points", postId, points);
    }
    
    // Phase 2.2: Check and increment bot reply count (Atomic operation)
    public boolean tryAddBotReply(Long postId) {
        String key = BOT_COUNT_PREFIX + postId + BOT_COUNT_SUFFIX;
        
        // Atomic check-and-increment using Lua script
        Long count = redisTemplate.execute(
            incrWithLimitScript,
            List.of(key),
            String.valueOf(MAX_BOT_REPLIES)
        );
        
        if (count != null && count > 0) {
            // Set TTL for the key if it's a new key (first bot reply)
            Boolean hasTTL = redisTemplate.getExpire(key, TimeUnit.SECONDS) > 0;
            if (!hasTTL) {
                redisTemplate.expire(key, Duration.ofDays(7)); // Auto-cleanup after a week
            }
            log.debug("Added bot reply to post {}. Count: {}/{}", postId, count, MAX_BOT_REPLIES);
            return true;
        } else {
            log.warn("Bot reply rejected for post {}. Max limit of {} reached", postId, MAX_BOT_REPLIES);
            return false;
        }
    }
    
    // Phase 2.2: Check comment depth limit
    public boolean isDepthAllowed(Integer depthLevel) {
        boolean allowed = depthLevel <= MAX_COMMENT_DEPTH;
        if (!allowed) {
            log.warn("Comment depth {} exceeds max limit of {}", depthLevel, MAX_COMMENT_DEPTH);
        }
        return allowed;
    }
    
    // Phase 2.2: Check cooldown between specific bot and human
    public boolean isBotHumanInteractionAllowed(Long botId, Long humanId) {
        String key = COOLDOWN_PREFIX + botId + ":human_" + humanId;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            log.warn("Bot {} cannot interact with human {} for {} more seconds", botId, humanId, ttl);
            return false;
        }
        return true;
    }
    
    // Phase 2.2: Set cooldown after bot-human interaction
    public void setBotHumanCooldown(Long botId, Long humanId) {
        String key = COOLDOWN_PREFIX + botId + ":human_" + humanId;
        redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(COOLDOWN_SECONDS));
        log.debug("Set cooldown for bot {} with human {} for {} seconds", botId, humanId, COOLDOWN_SECONDS);
    }
    
    // Phase 2.2: Get current bot reply count (for verification)
    public Long getBotReplyCount(Long postId) {
        String key = BOT_COUNT_PREFIX + postId + BOT_COUNT_SUFFIX;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.valueOf(count.toString()) : 0L;
    }
    
    // Phase 3.1: Check if user can receive immediate notification
    public boolean canReceiveImmediateNotification(Long userId) {
        String key = NOTIF_COOLDOWN_PREFIX + userId;
        return Boolean.FALSE.equals(redisTemplate.hasKey(key));
    }
    
    // Phase 3.1: Set notification cooldown and send immediate notification
    public void setNotificationCooldown(Long userId) {
        String key = NOTIF_COOLDOWN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(NOTIF_COOLDOWN_SECONDS));
        log.info("Push Notification Sent to User {}", userId);
    }
    
    // Phase 3.1: Queue pending notification
    public void queuePendingNotification(Long userId, String message) {
        String key = PENDING_NOTIF_PREFIX + userId + PENDING_NOTIF_SUFFIX;
        redisTemplate.opsForList().rightPush(key, message);
        log.debug("Queued pending notification for user {}: {}", userId, message);
    }
    
    // Phase 3.2: Get and clear pending notifications for a user
    public List<Object> getAndClearPendingNotifications(Long userId) {
        String key = PENDING_NOTIF_PREFIX + userId + PENDING_NOTIF_SUFFIX;
        
        // Get all pending messages
        List<Object> messages = redisTemplate.opsForList().range(key, 0, -1);
        
        // Delete the list
        redisTemplate.delete(key);
        
        return messages != null ? messages : List.of();
    }
    
    // Phase 3.2: Get all users with pending notifications
    public List<String> getAllUsersWithPendingNotifications() {
        String pattern = PENDING_NOTIF_PREFIX + "*" + PENDING_NOTIF_SUFFIX;
        return redisTemplate.keys(pattern).stream()
            .map(key -> {
                // Extract user ID from key pattern "user:{id}:pending_notifs"
                String[] parts = key.split(":");
                if (parts.length >= 2) {
                    return parts[1];
                }
                return null;
            })
            .filter(id -> id != null)
            .toList();
    }
    
    // Helper method to verify bot count consistency (for testing)
    public boolean verifyBotCount(Long postId, long expectedCount) {
        String key = BOT_COUNT_PREFIX + postId + BOT_COUNT_SUFFIX;
        Object actual = redisTemplate.opsForValue().get(key);
        long actualCount = actual != null ? Long.parseLong(actual.toString()) : 0;
        boolean isValid = actualCount == expectedCount;
        
        if (!isValid) {
            log.error("Bot count mismatch for post {}: Redis={}, Expected={}", 
                postId, actualCount, expectedCount);
        }
        return isValid;
    }
    
    public enum InteractionType {
        BOT_REPLY(1),
        HUMAN_LIKE(20),
        HUMAN_COMMENT(50);
        
        private final int points;
        
        InteractionType(int points) {
            this.points = points;
        }
        
        public int getPoints() {
            return points;
        }
    }
}