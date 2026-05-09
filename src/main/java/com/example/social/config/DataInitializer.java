package com.example.social.config;

import com.example.social.entity.Bot;
import com.example.social.entity.User;
import com.example.social.repository.BotRepository;
import com.example.social.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Create test users if none exist
        if (userRepository.count() == 0) {
            User user1 = User.builder()
                .username("john_doe")
                .isPremium(false)
                .build();
            User user2 = User.builder()
                .username("jane_smith")
                .isPremium(true)
                .build();
            
            userRepository.save(user1);
            userRepository.save(user2);
            
            log.info("Created test users: john_doe (ID: {}), jane_smith (ID: {})", user1.getId(), user2.getId());
        }
        
        // Create test bots if none exist
        if (botRepository.count() == 0) {
            Bot bot1 = Bot.builder()
                .name("HelpfulBot")
                .personaDescription("A helpful bot that answers questions")
                .build();
            Bot bot2 = Bot.builder()
                .name("FriendlyBot")
                .personaDescription("A friendly bot that engages in conversation")
                .build();
            Bot bot3 = Bot.builder()
                .name("NewsBot")
                .personaDescription("A bot that shares news updates")
                .build();
            
            botRepository.save(bot1);
            botRepository.save(bot2);
            botRepository.save(bot3);
            
            log.info("Created test bots: HelpfulBot (ID: {}), FriendlyBot (ID: {}), NewsBot (ID: {})", 
                bot1.getId(), bot2.getId(), bot3.getId());
        }
    }
}