package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.PermissionRequest;
import com.blur.userservice.identity.dto.response.PermissionResponse;
import com.blur.userservice.identity.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);

    PermissionRequest toPermissionRequest(Permission permission);

    PermissionResponse toPermissionResponse(Permission permission);
}
