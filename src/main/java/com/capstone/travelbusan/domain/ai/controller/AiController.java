package com.capstone.travelbusan.domain.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AiController {

    private final ChatClient chatClient;

    // ChatClient.Builder를 주입받아 빌드합니다.
    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/planner/generate")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }


}
