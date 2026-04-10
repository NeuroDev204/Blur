package com.blur.userservice.profile.dto.response;


import com.blur.userservice.profile.enums.RecommendationType;
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
