package com.earseo.core.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AiConfig {
    private final OpenAiChatModel openAiChatModel;

    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.3)
                .build();
    }

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(openAiChatOptions())
                .build();
    }
}
