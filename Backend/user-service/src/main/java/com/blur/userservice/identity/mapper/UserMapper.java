package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.identity.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toUser(UserCreationRequest userCreationRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest userUpdateRequest);

    @Mapping(target = "noPassword", source = "password", qualifiedByName = "toNoPassword")
    UserResponse toUserResponse(User user);

    @Named("toNoPassword")
    default Boolean toNoPassword(String password) {
        return password == null || password.isBlank();
    }
}
