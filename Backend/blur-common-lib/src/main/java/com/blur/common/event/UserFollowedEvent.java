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
public class UserFollowedEvent extends BaseEvent {

  // ===== NGƯỜI FOLLOW =====
  private String followerId;        // userId
  private String followerProfileId; // profile ID trong Neo4j
  private String followerName;
  private String followerFirstName;
  private String followerLastName;
  private String followerImageUrl;

  // ===== NGƯỜI ĐƯỢC FOLLOW =====
  private String followedUserId;
  private String followedProfileId;
  private String followedEmail;
  private String followedName;
}