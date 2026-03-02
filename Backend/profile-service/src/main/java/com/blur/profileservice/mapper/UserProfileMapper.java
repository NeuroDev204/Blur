package com.blur.profileservice.mapper;

import com.blur.profileservice.dto.request.ProfileCreationRequest;
import com.blur.profileservice.dto.request.UserProfileUpdateRequest;
import com.blur.profileservice.dto.response.UserProfileResponse;
import com.blur.profileservice.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    //    @Mapping(target = "dob", expression = "java(stringToLocalDate(request.getDob()))")
    void updateUserProfile(@MappingTarget UserProfile userProfile, UserProfileUpdateRequest request);

    UserProfileResponse toUserProfileResponse(UserProfile userProfile);

    default LocalDate stringToLocalDate(String dob) {
        if (dob == null || dob.isBlank()) return null;
        return LocalDate.parse(dob); // assuming format yyyy-MM-dd
    }

    UserProfile toUserProfile(ProfileCreationRequest request);
}
