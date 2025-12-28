package org.identityservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.identityservice.dto.request.PermissionRequest;
import org.identityservice.dto.response.PermissionResponse;
import org.identityservice.entity.Permission;
import org.identityservice.mapper.PermissionMapper;
import org.identityservice.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toPermissionResponse)
                .collect(Collectors.toList());
    }

    public void delete(String permissionName) {
        permissionRepository.deleteById(permissionName);
    }
}
