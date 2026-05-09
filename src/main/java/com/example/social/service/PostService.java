package com.example.social.service;

import com.example.social.dto.PostRequest;
import com.example.social.entity.Post;
import com.example.social.entity.User;
import com.example.social.entity.Bot;
import com.example.social.repository.PostRepository;
import com.example.social.repository.UserRepository;
import com.example.social.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final RedisGuardrailService redisGuardrailService;
    
    @Transactional
    public Post createPost(PostRequest request) {
        Post.AuthorType authorType = Post.AuthorType.valueOf(request.getAuthorType());
        
        // Verify author exists
        if (authorType == Post.AuthorType.HUMAN) {
            userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            botRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        }
        
        Post post = Post.builder()
            .content(request.getContent())
            .authorId(request.getAuthorId())
            .authorType(authorType)
            .likeCount(0)
            .build();
        
        Post savedPost = postRepository.save(post);
        log.info("Created post {} by {} {}", savedPost.getId(), authorType, request.getAuthorId());
        
        return savedPost;
    }
    
    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
        
        // Update virality score in Redis
        redisGuardrailService.incrementViralityScore(postId, RedisGuardrailService.InteractionType.HUMAN_LIKE);
        
        log.info("User {} liked post {}", userId, postId);
    }
    
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
    }
}