package com.blur.communicationservice.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.blur.communicationservice.entity.MediaAttachment;

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
