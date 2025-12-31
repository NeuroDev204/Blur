package com.blur.ai_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Getter
    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Getter
    @Value("${ai.openai.option.model}")
    private String model;

    @Getter
    @Value("${ai.openai.chat.base-url}")
    private String baseUrl;

    @Getter
    @Value("${ai.openai.chat.completions-path}")
    private String completionsPath;
}