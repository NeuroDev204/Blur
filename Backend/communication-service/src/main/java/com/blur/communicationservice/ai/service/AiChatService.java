package com.blur.communicationservice.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.blur.communicationservice.ai.config.GeminiConfig;
import com.blur.communicationservice.dto.request.AiChatRequest;
import com.blur.communicationservice.dto.response.AiChatResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiChatResponse chat(AiChatRequest request) {
        try {

            String url = geminiConfig.getBaseUrl() + "/" + geminiConfig.getCompletionsPath()
                    + "?key="
                    + geminiConfig.getApiKey();

            Map<String, Object> requestBody =
                    Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", request.getMessage())))));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> responseBody = response.getBody();
            String aiResponse = extractTextFromResponse(responseBody);

            AiChatResponse res = new AiChatResponse();
            res.setResponse(aiResponse);
            res.setSuccess(true);
            return res;

        } catch (Exception e) {
            AiChatResponse res = new AiChatResponse();
            res.setSuccess(false);
            res.setError(e.getMessage());
            return res;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map> candidates = (List<Map>) response.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "Khong the parse response";
        }
    }
}
