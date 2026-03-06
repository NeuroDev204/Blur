package com.blur.communicationservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.blur.communicationservice.dto.request.ChatMessageRequest;
import com.blur.communicationservice.dto.response.ChatMessageResponse;
import com.blur.communicationservice.entity.ChatMessage;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "me", ignore = true)
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "messageType", ignore = true)
    ChatMessage toChatMessage(ChatMessageRequest chatMessageRequest);

    List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages);
}
