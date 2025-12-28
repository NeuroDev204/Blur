package org.identityservice.mapper;

import org.identityservice.dto.request.UserCreationRequest;
import org.identityservice.dto.request.UserUpdateRequest;
import org.identityservice.dto.response.UserResponse;
import org.identityservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest userCreationRequest);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest userUpdateRequest);

    UserResponse toUserResponse(User user);
}
