package com.blur.userservice.profile.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    String id;
    String userId;
    String username;
    String firstName;
    String lastName;
    String bio;
    String city;
    String phone;
    String email;
    String gender;
    String website;
    String imageUrl;
    String address;
    LocalDate dob;
    LocalDate updatedAt;
    LocalDate createdAt;


}
