package com.blur.chatservice.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.blur.chatservice.entity.MediaAttachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ChatMessageRequest {
    @NotBlank
    String conversationId;

    @NotBlank
    String message;

    List<MediaAttachment> attachments;
}
