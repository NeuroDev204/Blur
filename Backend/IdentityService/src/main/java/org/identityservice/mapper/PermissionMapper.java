package org.identityservice.mapper;

import org.identityservice.dto.request.PermissionRequest;
import org.identityservice.dto.response.PermissionResponse;
import org.identityservice.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);

    PermissionRequest toPermissionRequest(Permission permission);

    PermissionResponse toPermissionResponse(Permission permission);
}
