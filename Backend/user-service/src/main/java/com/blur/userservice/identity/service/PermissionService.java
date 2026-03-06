package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.PermissionRequest;
import com.blur.userservice.identity.dto.response.PermissionResponse;
import com.blur.userservice.identity.entity.Permission;
import com.blur.userservice.identity.mapper.PermissionMapper;
import com.blur.userservice.identity.repository.PermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
