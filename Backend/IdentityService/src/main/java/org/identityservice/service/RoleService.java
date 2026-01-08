package org.identityservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.identityservice.dto.request.RoleRequest;
import org.identityservice.dto.response.RoleResponse;
import org.identityservice.mapper.RoleMapper;
import org.identityservice.repository.PermissionRepository;
import org.identityservice.repository.RoleRepository;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponse createRole(RoleRequest roleRequest) {
        var role = roleMapper.toRole(roleRequest);
        var permissions = permissionRepository.findAllById(roleRequest.getPermissions());
        role.setPermissions(new HashSet<>(permissions));
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
