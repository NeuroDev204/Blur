package com.blur.userservice.profile.mapper;

import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.dto.request.UserProfileUpdateRequest;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

// IGNORE null source values on update so a partial update (e.g. only imageUrl) does NOT
// overwrite existing fields (username, firstName, ...) with null. Without this, editing
// one field wipes all others — which corrupted the blur user's username and broke auto-follow.
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
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
