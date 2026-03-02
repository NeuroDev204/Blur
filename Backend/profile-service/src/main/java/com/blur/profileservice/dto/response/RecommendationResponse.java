package com.blur.profileservice.dto.response;


import com.blur.profileservice.enums.RecommendationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationResponse {
  String id;
  String userId;
  String username;
  String firstName;
  String lastName;
  String imageUrl;
  String bio;
  String city;
  Integer followerCount;
  Integer followingCount;
  Integer mutualConnections;
  RecommendationType recommendationType;
}
