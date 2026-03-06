package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.RoleRequest;
import com.blur.userservice.identity.dto.response.RoleResponse;
import com.blur.userservice.identity.entity.Permission;
import com.blur.userservice.identity.entity.Role;
import com.blur.userservice.identity.mapper.RoleMapper;
import com.blur.userservice.identity.repository.PermissionRepository;
import com.blur.userservice.identity.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponse createRole(RoleRequest roleRequest) {
        Role role = roleMapper.toRole(roleRequest);
        var permissions = permissionRepository.findAllById(roleRequest.getPermissions());
        role.setPermissions(new HashSet<Permission>(permissions));
        roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).collect(Collectors.toList());
    }

    public RoleResponse getRoleById(String roleName) {
        var role = roleRepository.findById(roleName).orElse(null);
        return roleMapper.toRoleResponse(role);
    }

    public void deleteRoleById(String roleName) {
        roleRepository.deleteById(roleName);
    }
}
