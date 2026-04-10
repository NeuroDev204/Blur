package com.blur.userservice.profile.service;

import com.blur.userservice.profile.crypto.FieldEncryptionService;
import com.blur.userservice.profile.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.profile.dto.request.UserCreationRequest;
import com.blur.userservice.profile.dto.request.UserUpdateRequest;
import com.blur.userservice.profile.dto.response.UserResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.exception.AppException;
import com.blur.userservice.profile.exception.ErrorCode;
import com.blur.userservice.profile.mapper.UserMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.repository.httpclient.ContentServiceClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserProfileRepository userProfileRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    ContentServiceClient contentServiceClient;
    KeycloakUserService keycloakUserService;
    FieldEncryptionService fieldEncryptionService;

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userProfileRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (StringUtils.hasText(request.getEmail())) {
            String emailIndex = fieldEncryptionService.blindIndex(request.getEmail());
            if (userProfileRepository.findByEmailIndex(emailIndex).isPresent()) {
                throw new AppException(ErrorCode.USER_EXISTED);

            }
        }

        // tao userProfile
        UserProfile userProfile = userMapper.toUserProfile(request);
        String userId = UUID.randomUUID().toString();
        userProfile.setUserId(userId);
        userProfile.setUsername(request.getUsername());
        userProfile.setEmail(request.getEmail()); //converter tu encrypt
        userProfile.setEmailIndex(fieldEncryptionService.blindIndex(request.getEmail()));
        userProfile.setPasswordHash(null); // password chi luu trong keycloak
        userProfile.setRoles(List.of("USER"));
        userProfile.setEmailVerified(false);
        userProfile.setCreatedAt(LocalDate.now());
        try {
            userProfile = userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // tao user trong keycloak
        String keycloakUserId = null;
        try {
            keycloakUserId = keycloakUserService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(), // keycloak tu hash
                    request.getFirstName(),
                    request.getLastName(),
                    userId,  // blurUserId → JWT sub claim
                    List.of("USER")
            );
        } catch (Exception e) {
            log.error("Failed to create Keycloak user, rolling back Neo4j: {}", e.getMessage());
            userProfileRepository.deleteByUserId(userId);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // tu dong follow user "blur" khi dang ky + backfill feed
        final UserProfile savedProfile = userProfile;
        final String finalKeycloakUserId = keycloakUserId;
        try {
            userProfileRepository.findByUsername("blur").ifPresent(blurUser -> {
                if (!savedProfile.getId().equals(blurUser.getId())) {
                    userProfileRepository.follow(savedProfile.getId(), blurUser.getId());
                    userProfileRepository.updateFollowCounts(savedProfile.getId());
                    userProfileRepository.updateFollowCounts(blurUser.getId());
                    try {
                        contentServiceClient.backfillFeed(savedProfile.getUserId(), blurUser.getUserId());
                    } catch (Exception e) {
                        log.warn("Feed backfill failed for new user {}: {}", savedProfile.getUserId(), e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Follow/backfill failed for new user {}, rolling back: {}", userId, e.getMessage());
            userProfileRepository.deleteByUserId(userId);
            keycloakUserService.deleteUser(finalKeycloakUserId);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
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
    @Cacheable(value = "users", sync = true)
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userProfileRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Cacheable(value = "userById", key = "#userId", sync = true)
    @Transactional(readOnly = true)
    public UserProfile getUserById(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "myInfo", allEntries = true)
    })
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

    @Cacheable(value = "myInfo", key = "#root.target.getCurrentUsername()", sync = true)
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
