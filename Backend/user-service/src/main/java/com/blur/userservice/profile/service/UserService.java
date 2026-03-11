package com.blur.userservice.profile.service;

import com.blur.userservice.profile.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.profile.dto.request.UserCreationRequest;
import com.blur.userservice.profile.dto.request.UserUpdateRequest;
import com.blur.userservice.profile.dto.response.UserResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.exception.AppException;
import com.blur.userservice.profile.exception.ErrorCode;
import com.blur.userservice.profile.mapper.UserMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserProfileRepository userProfileRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userById", key = "#result.id", condition = "#result != null")
    })
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userProfileRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (StringUtils.hasText(request.getEmail())
                && userProfileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserProfile userProfile = userMapper.toUserProfile(request);
        userProfile.setUserId(UUID.randomUUID().toString());
        userProfile.setUsername(request.getUsername());
        userProfile.setEmail(request.getEmail());
        userProfile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userProfile.setRoles(List.of("USER"));
        userProfile.setEmailVerified(false);
        userProfile.setCreatedAt(LocalDate.now());
        try {
            userProfile = userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(userProfile);
    }

    public void createPassword(UserCreationPasswordRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (StringUtils.hasText(userProfile.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_EXISTED);
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        userProfile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userProfileRepository.save(userProfile);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userProfileRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Cacheable(value = "userById", key = "#userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserProfile getUserById(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public UserProfile updateUser(String userId, UserUpdateRequest request) {
        UserProfile userProfile = getUserById(userId);
        userMapper.updateUser(userProfile, request);
        if (StringUtils.hasText(request.getPassword())) {
            userProfile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            userProfile.setRoles(request.getRoles());
        }
        return userProfileRepository.save(userProfile);
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "myInfo", key = "#root.target.getCurrentUsername()")
    })
    public void deleteUser(String userId) {
        UserProfile userProfile = getUserById(userId);
        userProfileRepository.deleteById(userProfile.getId());
    }

    @Cacheable(value = "myInfo", key = "#root.target.getCurrentUsername()", unless = "#result == null ")
    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(userProfile);
        userResponse.setNoPassword(!StringUtils.hasText(userProfile.getPasswordHash()));
        return userResponse;
    }

    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
