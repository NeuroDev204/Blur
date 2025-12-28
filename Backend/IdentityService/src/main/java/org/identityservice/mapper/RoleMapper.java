package org.identityservice.mapper;

import org.identityservice.dto.request.RoleRequest;
import org.identityservice.dto.response.RoleResponse;
import org.identityservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest roleRequest);

    RoleResponse toRoleResponse(Role role);
}
