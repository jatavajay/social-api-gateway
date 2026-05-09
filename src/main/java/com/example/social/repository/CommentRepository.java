package com.example.social.repository;

import com.example.social.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.authorType = 'BOT'")
    long countBotCommentsByPostId(@Param("postId") Long postId);
}