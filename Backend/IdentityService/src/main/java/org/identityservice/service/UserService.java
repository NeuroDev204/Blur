package org.identityservice.service;

import java.util.HashSet;
import java.util.List;

import org.identityservice.dto.request.UserCreationPasswordRequest;
import org.identityservice.dto.request.UserCreationRequest;
import org.identityservice.dto.request.UserUpdateRequest;
import org.identityservice.entity.Role;
import org.identityservice.entity.User;
import org.identityservice.mapper.ProfileMapper;
import org.identityservice.mapper.UserMapper;
import org.identityservice.repository.RoleRepository;
import org.identityservice.repository.UserRepository;
import org.identityservice.repository.httpclient.ProfileClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.blur.common.dto.response.UserResponse;
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    ProfileClient profileClient;
    ProfileMapper profileMapper;
    RoleRepository roleRepository;

    @Caching(
            evict = {
                @CacheEvict(value = "users", allEntries = true),
                @CacheEvict(value = "userById", key = "#result.id", condition = "#result != null")
            })
    public UserResponse createUser(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById("USER").ifPresent(roles::add);
        user.setRoles(roles);
        user.setEmailVerified(false);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BlurException(ErrorCode.USER_EXISTED);
        }
        // tao profile tu user da nhan
        var profileResponse = profileMapper.toProfileCreationRequest(request);
        profileResponse.setUsername(user.getUsername());
        // mapping userid tu user vao profile
        profileResponse.setUserId(user.getId());
        profileResponse.setEmail(user.getEmail());
        profileClient.createProfile(profileResponse);
        // build notification event

        var userResponse = userMapper.toUserResponse(user);

        return userResponse;
    }

    @Caching(
            evict = {
                @CacheEvict(value = "users", allEntries = true),
                @CacheEvict(value = "userById", key = "#result.id", condition = "#result != null")
            })
    public void createUsers(UserCreationRequest request) {
        for (int i = 1; i <= 10000; i++) {
            User user = userMapper.toUser(request);
            user.setUsername(request.getUsername() + i);
            user.setEmail(user.getEmail() + i);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            HashSet<Role> roles = new HashSet<>();
            roleRepository.findById("USER").ifPresent(roles::add);
            user.setRoles(roles);
            user.setEmailVerified(false);
            try {
                userRepository.save(user);

            } catch (DataIntegrityViolationException ex) {
                throw new BlurException(ErrorCode.USER_EXISTED);
            }
            // tao profile tu user da nhan
            var profileResponse = profileMapper.toProfileCreationRequest(request);
            profileResponse.setUsername(user.getUsername());
            // mapping userid tu user vao profile
            profileResponse.setUserId(user.getId());
            profileResponse.setEmail(user.getEmail());
            profileClient.createProfile(profileResponse);
            // build notification event

            var userResponse = userMapper.toUserResponse(user);
        }
    }

    // Create Password for google
    public void createPassword(UserCreationPasswordRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName(); // subject = userId

        User user = userRepository.findById(userId).orElseThrow(() -> new BlurException(ErrorCode.USER_NOT_EXISTED));

        // User đã có password rồi thì không cho tạo lại
        if (StringUtils.hasText(user.getPassword())) {
            throw new BlurException(ErrorCode.PASSWORD_EXISTED);
        }

        // Password mới bắt buộc phải có
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BlurException(ErrorCode.UNAUTHENTICATED); // hoặc ErrorCode.INVALID_PASSWORD
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", unless = "#result == null || #result.isEmpty()")
    public List<UserResponse> getUsers() {
        log.info("Getting users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @Cacheable(value = "userById", key = "#userId", unless = "#result == null")
    public User getUserById(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BlurException(ErrorCode.USER_NOT_EXISTED));
    }

    public User updateUser(String userId, UserUpdateRequest request) {
        User user = getUserById(userId);
        userMapper.updateUser(user, request);
        return userRepository.save(user);
    }

    @Caching(
            evict = {
                @CacheEvict(value = "users", allEntries = true),
                @CacheEvict(value = "userById", key = "#userId"),
                @CacheEvict(value = "myInfo", key = "#root.target.getUsernameById(#userId)")
            })
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @Cacheable(value = "myInfo", key = "#root.target.getCurrentUsername()", unless = "#result == null ")
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        // subject trong JWT đang là user.getId()
        String userId = context.getAuthentication().getName();

        // Vì subject = userId => phải findById
        User user = userRepository.findById(userId).orElseThrow(() -> new BlurException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(user);
        return userResponse;
    }
}
