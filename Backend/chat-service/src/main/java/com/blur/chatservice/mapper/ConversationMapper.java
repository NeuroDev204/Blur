package com.blur.chatservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.blur.chatservice.dto.response.ConversationResponse;
import com.blur.chatservice.entity.Conversation;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    ConversationResponse toConversationResponse(Conversation conversation);

    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
}
