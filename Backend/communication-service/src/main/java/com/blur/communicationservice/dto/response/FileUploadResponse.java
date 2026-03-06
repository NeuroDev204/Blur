package com.blur.communicationservice.dto.response;

import com.blur.communicationservice.entity.MediaAttachment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {
    Boolean success;
    MediaAttachment attachment;
    String message;
}
