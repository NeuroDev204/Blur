package com.blur.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostLikedEvent extends BaseEvent {
  String postId;

  String likerId;
  String likerName;
  String likerFirstName;
  String likerLastName;
  String likerImageUrl;

  String postOwnerId;
  String postOwnerEmail;
  String postOwnerName;
  
}
