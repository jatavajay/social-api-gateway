package com.example.social.service;

import com.example.social.dto.CommentRequest;
import com.example.social.entity.Comment;
import com.example.social.entity.Post;
import com.example.social.repository.CommentRepository;
import com.example.social.repository.PostRepository;
import com.example.social.repository.UserRepository;
import com.example.social.service.NotificationService;
import com.example.social.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final RedisGuardrailService redisGuardrailService;
    private final NotificationService notificationService;
    
    @Transactional
    public Comment addComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Comment.AuthorType authorType = Comment.AuthorType.valueOf(request.getAuthorType());
        Long authorId = request.getAuthorId();
        
        // Calculate depth level
        int depthLevel = calculateDepthLevel(request.getParentCommentId());
        
        // Phase 2.2: Check depth cap (Vertical Cap)
        if (!redisGuardrailService.isDepthAllowed(depthLevel)) {
            throw new RuntimeException("Comment depth cannot exceed 20 levels");
        }
        
        // Phase 2.2: Check cooldown for bot interactions
        boolean isBot = authorType == Comment.AuthorType.BOT;
        boolean isReplyToHuman = false;
        Long humanAuthorId = null;
        
        if (isBot && post.getAuthorType() == Post.AuthorType.HUMAN) {
            humanAuthorId = post.getAuthorId();
            isReplyToHuman = true;
            
            // Check cooldown
            if (!redisGuardrailService.isBotHumanInteractionAllowed(authorId, humanAuthorId)) {
                throw new RuntimeException("Bot cannot interact with this human more than once per 10 minutes");
            }
        }
        
        // Phase 2.2: Check horizontal cap for bot replies (Atomic operation)
        if (isBot) {
            boolean allowed = redisGuardrailService.tryAddBotReply(postId);
            if (!allowed) {
                throw new RuntimeException("Post cannot have more than 100 bot replies");
            }
        }
        
        // Verify author exists
        if (authorType == Comment.AuthorType.HUMAN) {
            userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            botRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        }
        
        // Create and save comment
        Comment comment = Comment.builder()
            .post(post)
            .authorId(authorId)
            .authorType(authorType)
            .content(request.getContent())
            .depthLevel(depthLevel)
            .parentCommentId(request.getParentCommentId())
            .build();
        
        Comment savedComment = commentRepository.save(comment);
        
        // Phase 2.1: Update virality score based on interaction type
        if (isBot) {
            redisGuardrailService.incrementViralityScore(postId, RedisGuardrailService.InteractionType.BOT_REPLY);
        } else {
            redisGuardrailService.incrementViralityScore(postId, RedisGuardrailService.InteractionType.HUMAN_COMMENT);
        }
        
        // Set cooldown after bot interaction
        if (isBot && isReplyToHuman && humanAuthorId != null) {
            redisGuardrailService.setBotHumanCooldown(authorId, humanAuthorId);
            
            // Phase 3: Handle notification for bot reply
            String botName = botRepository.findById(authorId).map(bot -> bot.getName()).orElse("Bot " + authorId);
            notificationService.handleBotReplyNotification(post.getAuthorId(), postId, botName);
        }
        
        log.info("Added {} comment by {} {} to post {} at depth {}", 
            authorType, authorType, authorId, postId, depthLevel);
        
        return savedComment;
    }
    
    private int calculateDepthLevel(Long parentCommentId) {
        if (parentCommentId == null) {
            return 1; // Root level comment
        }
        
        // Find parent comment to get its depth
        Comment parentComment = commentRepository.findById(parentCommentId)
            .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        
        return parentComment.getDepthLevel() + 1;
    }
}