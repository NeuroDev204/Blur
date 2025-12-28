package org.identityservice.controller;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.Valid;

import org.identityservice.dto.request.ApiResponse;
import org.identityservice.dto.request.UserCreationPasswordRequest;
import org.identityservice.dto.request.UserCreationRequest;
import org.identityservice.dto.request.UserUpdateRequest;
import org.identityservice.dto.response.UserResponse;
import org.identityservice.entity.User;
import org.identityservice.exception.AppException;
import org.identityservice.exception.ErrorCode;
import org.identityservice.mapper.UserMapper;
import org.identityservice.repository.RoleRepository;
import org.identityservice.repository.UserRepository;
import org.identityservice.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserController {
    UserService userService;
    UserMapper userMapper;
    RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // Create for google
    @PostMapping("/create-password")
    public ApiResponse<Void> createPassword(@RequestBody @Valid UserCreationPasswordRequest request) {
        userService.createPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Password has been created, you could use it to login")
                .build();
    }

    @PostMapping("/registration")
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {

        var result = userService.createUser(request);
        return ApiResponse.<UserResponse>builder().code(1000).result(result).build();
    }

    @PostMapping("/registrations")
    public ApiResponse<?> createUsers(@RequestBody @Valid UserCreationRequest request) {
        userService.createUser(request);
        return ApiResponse.builder().result(true).build();
    }

    @GetMapping("/all")
    public ApiResponse<List<UserResponse>> getUsers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("username: {}", authentication.getName());
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> myInfo() {
        return ApiResponse.<UserResponse>builder()
                .code(1000) // Phía FE đang kiểm tra 1000
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable String userId) {
        var userResponse = userMapper.toUserResponse(userService.getUserById(userId));
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userResponse)
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));
        return ApiResponse.<UserResponse>builder()
                .result(userMapper.toUserResponse(userRepository.save(user)))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }
}
