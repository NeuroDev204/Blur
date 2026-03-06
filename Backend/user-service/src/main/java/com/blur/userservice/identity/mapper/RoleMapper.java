package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.RoleRequest;
import com.blur.userservice.identity.dto.response.RoleResponse;
import com.blur.userservice.identity.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest roleRequest);

    RoleResponse toRoleResponse(Role role);
}
