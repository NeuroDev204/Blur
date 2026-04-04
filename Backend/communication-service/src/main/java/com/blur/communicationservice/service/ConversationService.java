package com.blur.communicationservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.blur.communicationservice.chat.repository.ChatMessageRepository;
import com.blur.communicationservice.chat.repository.ConversationRepository;
import com.blur.communicationservice.dto.request.ConversationRequest;
import com.blur.communicationservice.dto.response.ConversationResponse;
import com.blur.communicationservice.entity.ChatMessage;
import com.blur.communicationservice.entity.Conversation;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.enums.MessageType;
import com.blur.communicationservice.exception.AppException;
import com.blur.communicationservice.exception.ErrorCode;
import com.blur.communicationservice.mapper.ConversationMapper;
import com.blur.communicationservice.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {

    ConversationMapper conversationMapper;
    ProfileClient profileClient;
    ConversationRepository conversationRepository;
    ChatMessageRepository chatMessageRepository;

    private String getAuthenticatedUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!StringUtils.hasText(userId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userId;
    }

    public List<ConversationResponse> myConversations() {
        String userId = getAuthenticatedUserId();
        var userResponse = profileClient.getProfile(userId);

        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(
                userResponse.getResult().getUserId());

        return conversations.stream()
                .map(this::toConversationResponseWithLastMessage)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponse createConversation(ConversationRequest request) {
        String userId = getAuthenticatedUserId();
        var userInfoResponse = profileClient.getProfile(userId);
        var participantInfoResponse =
                profileClient.getProfile(request.getParticipantIds().get(0));

        if (Objects.isNull(userInfoResponse) || Objects.isNull(participantInfoResponse)) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        var userInfo = userInfoResponse.getResult();
        var participantInfo = participantInfoResponse.getResult();

        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        userIds.add(participantInfo.getUserId());

        var sortedIds = userIds.stream().sorted().toList();
        String userIdHash = generateParticipantHash(sortedIds);

        var conversation = conversationRepository
                .findByParticipantsHash(userIdHash)
                .orElseGet(() -> {
                    List<ParticipantInfo> participantInfos = List.of(
                            ParticipantInfo.builder()
                                    .userId(userInfo.getUserId())
                                    .username(userInfo.getUsername())
                                    .firstName(userInfo.getFirstName())
                                    .lastName(userInfo.getLastName())
                                    .avatar(userInfo.getImageUrl())
                                    .build(),
                            ParticipantInfo.builder()
                                    .userId(participantInfo.getUserId())
                                    .username(participantInfo.getUsername())
                                    .firstName(participantInfo.getFirstName())
                                    .lastName(participantInfo.getLastName())
                                    .avatar(participantInfo.getImageUrl())
                                    .build());

                    Conversation newConversation = Conversation.builder()
                            .type(request.getType())
                            .participantsHash(userIdHash)
                            .createdDate(Instant.now())
                            .modifiedDate(Instant.now())
                            .participants(participantInfos)
                            .build();
                    return conversationRepository.save(newConversation);
                });

        return toConversationResponse(conversation);
    }

    @Transactional
    public String deleteConversation(String conversationId) {
        conversationRepository.deleteById(conversationId);
        return "Deleted conversation successfully";
    }

    private ConversationResponse toConversationResponseWithLastMessage(Conversation conversation) {
        String currentUserId = getAuthenticatedUserId();
        var profileResponse = profileClient.getProfile(currentUserId);

        ConversationResponse response = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo
                        .getUserId()
                        .equals(profileResponse.getResult().getUserId()))
                .findFirst()
                .ifPresent(participantInfo -> {
                    response.setConversationName(participantInfo.getFirstName() + " " + participantInfo.getLastName());
                    response.setConversationAvatar(participantInfo.getAvatar());
                });

        ChatMessage lastMessage = getLastMessageCached(conversation.getId());

        if (lastMessage != null) {
            response.setLastMessage(formatLastMessage(lastMessage));
            response.setLastMessageTime(lastMessage.getCreatedDate());

            if (lastMessage.getSender() != null) {
                String senderName = lastMessage.getSender().getFirstName() + " "
                        + lastMessage.getSender().getLastName();
                response.setLastMessageSender(senderName.trim());
            }
        }

        return response;
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId = getAuthenticatedUserId();
        var profileResponse = profileClient.getProfile(currentUserId);

        ConversationResponse response = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo
                        .getUserId()
                        .equals(profileResponse.getResult().getUserId()))
                .findFirst()
                .ifPresent(participantInfo -> {
                    response.setConversationName(participantInfo.getFirstName() + " " + participantInfo.getLastName());
                    response.setConversationAvatar(participantInfo.getAvatar());
                });

        return response;
    }

    private ChatMessage getLastMessageCached(String conversationId) {
        try {
            return chatMessageRepository.findFirstByConversationIdOrderByCreatedDateDesc(conversationId);
        } catch (Exception e) {
            return null;
        }
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private String formatLastMessage(ChatMessage message) {
        if (message.getMessage() != null && !message.getMessage().isEmpty()) {
            String content = message.getMessage();
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }

        MessageType type = message.getMessageType();
        if (type == null) {
            return "Tin nhan";
        }

        switch (type) {
            case IMAGE:
                return "Hinh anh";
            case VIDEO:
                return "Video";
            case FILE:
                return "Tep dinh kem";
            case MIXED:
                return "Tin nhan co dinh kem";
            case TEXT:
            default:
                return "Tin nhan";
        }
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner joiner = new StringJoiner("_");
        ids.forEach(joiner::add);
        return joiner.toString();
    }

    @Transactional
    public ConversationResponse toggleAI(String conversationId, Boolean enabled) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        var conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        if (!isParticipant) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        conversation.setAiEnabled(enabled);

        if (!enabled) {
            conversation.setAiConversationId(null);
        }

        conversation = conversationRepository.save(conversation);

        return toConversationResponse(conversation);
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
