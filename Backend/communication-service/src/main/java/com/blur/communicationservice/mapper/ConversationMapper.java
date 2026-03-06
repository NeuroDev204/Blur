package com.blur.communicationservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.blur.communicationservice.dto.response.ConversationResponse;
import com.blur.communicationservice.entity.Conversation;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    ConversationResponse toConversationResponse(Conversation conversation);

    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
}
