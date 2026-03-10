package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.identity.dto.response.RoleResponse;
import com.blur.userservice.identity.dto.response.PermissionResponse;
import com.blur.userservice.identity.entity.User;
import com.blur.userservice.identity.entity.Role;
import com.blur.userservice.identity.entity.Permission;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

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
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToRoleResponses")
    UserResponse toUserResponse(User user);

    @Named("toNoPassword")
    default Boolean toNoPassword(String password) {
        return password == null || password.isBlank();
    }

    @Named("rolesToRoleResponses")
    default Set<RoleResponse> rolesToRoleResponses(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> RoleResponse.builder()
                        .name(role.getName())
                        .description(role.getDescription())
                        .permissions(permissionsToPermissionResponses(role.getPermissions()))
                        .build())
                .collect(Collectors.toSet());
    }

    @Named("permissionsToPermissionResponses")
    default Set<PermissionResponse> permissionsToPermissionResponses(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .map(permission -> PermissionResponse.builder()
                        .name(permission.getName())
                        .description(permission.getDescription())
                        .build())
                .collect(Collectors.toSet());
    }
}
