package com.blur.common.event;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostCreatedEvent extends BaseEvent {
  String postId;
  String authorId;
  String authorName;
  String content;
  List<String> mediaUrls;
}
