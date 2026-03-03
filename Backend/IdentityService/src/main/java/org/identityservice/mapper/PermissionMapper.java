package org.identityservice.mapper;

import org.identityservice.dto.request.PermissionRequest;
import org.identityservice.dto.response.PermissionResponse;
import org.identityservice.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);

    PermissionRequest toPermissionRequest(Permission permission);

    PermissionResponse toPermissionResponse(Permission permission);
}
