package com.blur.chatservice.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MediaAttachment {
    String id;
    String url;
    String fileName;
    String fileType;
    Long fileSize;
    Integer width;
    Integer height;
    Integer duration;
    String thumbnailUrl;
}
