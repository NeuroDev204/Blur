package com.blur.chatservice.dto.response;

import com.blur.chatservice.entity.MediaAttachment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {
    Boolean success;
    MediaAttachment attachment;
    String message;
}
