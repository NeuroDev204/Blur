package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.profile.dto.request.UserCreationRequest;
import com.blur.userservice.profile.dto.request.UserUpdateRequest;
import com.blur.userservice.profile.dto.response.UserResponse;
import com.blur.userservice.profile.mapper.UserMapper;
import com.blur.userservice.profile.service.UserDeleteSagaService;
import com.blur.userservice.profile.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserController {
    UserService userService;
    UserMapper userMapper;
    UserDeleteSagaService userDeleteSagaService;

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
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> myInfo() {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
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
        var updated = userService.updateUser(userId, request);
        return ApiResponse.<UserResponse>builder()
                .result(userMapper.toUserResponse(updated))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable String userId) {
        // userService.deleteUser(userId);
        userDeleteSagaService.initiateDeleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }
}
