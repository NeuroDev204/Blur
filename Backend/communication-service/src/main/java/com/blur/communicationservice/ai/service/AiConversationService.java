package com.blur.communicationservice.ai.service;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.blur.communicationservice.ai.entity.AiChatMessage;
import com.blur.communicationservice.ai.entity.AiConversation;
import com.blur.communicationservice.ai.repository.AiConversationRepository;
import com.blur.communicationservice.dto.request.AiChatRequest;
import com.blur.communicationservice.dto.response.AiChatResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiChatService aiChatService;
    private final AiConversationRepository repository;

    public AiChatResponse chat(AiChatRequest request) {

        AiConversation conversation;

        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            conversation = new AiConversation();
            conversation.setUserId(request.getUserId());
            conversation.setCreatedAt(Instant.now());
            conversation.setMessages(new ArrayList<>());
        } else {
            conversation = repository.findById(request.getConversationId()).orElseThrow();
        }

        // Luu message cua user
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        userMsg.setTimestamp(Instant.now());
        conversation.getMessages().add(userMsg);

        // Goi AI (Gemini)
        AiChatResponse aiResult = aiChatService.chat(request);
        String reply = aiResult.getResponse();

        // Neu call AI loi thi van luu history, nhung tra loi ra ngoai
        if (!aiResult.isSuccess()) {
            AiChatMessage aiError = new AiChatMessage();
            aiError.setRole("assistant");
            aiError.setContent("AI error: " + aiResult.getError());
            aiError.setTimestamp(Instant.now());
            conversation.getMessages().add(aiError);

            conversation.setUpdatedAt(Instant.now());
            conversation = repository.save(conversation);

            AiChatResponse errorRes = new AiChatResponse();
            errorRes.setConversationId(conversation.getId());
            errorRes.setResponse(aiError.getContent());
            errorRes.setSuccess(false);
            errorRes.setError(aiResult.getError());
            return errorRes;
        }

        // Luu message AI
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setRole("assistant");
        aiMsg.setContent(reply);
        aiMsg.setTimestamp(Instant.now());
        conversation.getMessages().add(aiMsg);

        // Update DB
        conversation.setUpdatedAt(Instant.now());
        conversation = repository.save(conversation);

        // Tra response
        AiChatResponse successRes = new AiChatResponse();
        successRes.setConversationId(conversation.getId());
        successRes.setResponse(reply);
        successRes.setSuccess(true);
        return successRes;
    }
}
