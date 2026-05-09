package com.example.social.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_post_id", columnList = "post_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    
    @Column(name = "author_type")
    @Enumerated(EnumType.STRING)
    private AuthorType authorType;
    
    @Column(nullable = false, length = 1000)
    private String content;
    
    @Column(name = "depth_level", nullable = false)
    private Integer depthLevel;
    
    @Column(name = "parent_comment_id")
    private Long parentCommentId;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum AuthorType {
        HUMAN, BOT
    }
}