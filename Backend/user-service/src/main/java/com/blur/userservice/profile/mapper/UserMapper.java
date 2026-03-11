package com.blur.userservice.profile.mapper;

import com.blur.userservice.profile.dto.request.UserCreationRequest;
import com.blur.userservice.profile.dto.request.UserUpdateRequest;
import com.blur.userservice.profile.dto.response.UserResponse;
import com.blur.userservice.profile.entity.UserProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UserProfile toUserProfile(UserCreationRequest userCreationRequest);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    void updateUser(@MappingTarget UserProfile userProfile, UserUpdateRequest userUpdateRequest);

    @Mapping(target = "id", source = "userId")
    @Mapping(target = "noPassword", source = "passwordHash", qualifiedByName = "toNoPassword")
    UserResponse toUserResponse(UserProfile userProfile);

    @Named("toNoPassword")
    default Boolean toNoPassword(String passwordHash) {
        return passwordHash == null || passwordHash.isBlank();
    }
}
