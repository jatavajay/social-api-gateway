package com.example.social.controller;

import com.example.social.dto.ApiResponse;
import com.example.social.dto.CommentRequest;
import com.example.social.dto.PostRequest;
import com.example.social.entity.Comment;
import com.example.social.entity.Post;
import com.example.social.service.CommentService;
import com.example.social.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final CommentService commentService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Post>> createPost(@Valid @RequestBody PostRequest request) {
        try {
            Post post = postService.createPost(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", post));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Comment>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        try {
            Comment comment = commentService.addComment(postId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", comment));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("100 bot replies")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("depth")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("cooldown") || e.getMessage().contains("10 minutes")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @PathVariable Long postId,
            @RequestParam Long userId) {
        try {
            postService.likePost(postId, userId);
            return ResponseEntity.ok(ApiResponse.success("Post liked successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Post>> getPost(@PathVariable Long postId) {
        try {
            Post post = postService.getPost(postId);
            return ResponseEntity.ok(ApiResponse.success(post));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}