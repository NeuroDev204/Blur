package com.blur.communicationservice.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class GeminiConfig {

    @Getter
    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Getter
    @Value("${ai.gemini.option.model}")
    private String model;

    @Getter
    @Value("${ai.gemini.chat.base-url}")
    private String baseUrl;

    @Getter
    @Value("${ai.gemini.chat.completions-path}")
    private String completionsPath;
}
