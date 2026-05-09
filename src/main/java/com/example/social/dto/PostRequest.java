package com.example.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    
    @NotBlank(message = "Content cannot be blank")
    private String content;
    
    @NotNull(message = "Author ID is required")
    private Long authorId;
    
    private String authorType; // "HUMAN" or "BOT"
}