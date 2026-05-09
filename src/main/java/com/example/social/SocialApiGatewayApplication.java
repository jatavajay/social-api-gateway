package com.example.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SocialApiGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SocialApiGatewayApplication.class, args);
        System.out.println("🚀 Social API Gateway started successfully!");
        System.out.println("📝 API endpoints available at: http://localhost:8080/api/");
        System.out.println("💾 PostgreSQL: localhost:5432");
        System.out.println("⚡ Redis: localhost:6379");
    }
}