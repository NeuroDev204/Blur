package org.identityservice.controller;

import java.util.List;

import org.identityservice.dto.request.ApiResponse;
import org.identityservice.dto.request.PermissionRequest;
import org.identityservice.dto.response.PermissionResponse;
import org.identityservice.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@RestController
@Data
@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/permissions")
public class PermissionController {
    PermissionService permissionService;

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@RequestBody PermissionRequest permissionRequest) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(permissionRequest))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> findAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.findAll())
                .build();
    }

    @DeleteMapping("/{permissionId}")
    public ApiResponse<Void> deletePermission(@PathVariable String permissionId) {
        permissionService.delete(permissionId);
        return ApiResponse.<Void>builder().build();
    }
}
