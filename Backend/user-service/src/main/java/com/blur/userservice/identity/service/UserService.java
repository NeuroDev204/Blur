package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.identity.entity.Role;
import com.blur.userservice.identity.entity.User;
import com.blur.userservice.identity.exception.AppException;
import com.blur.userservice.identity.exception.ErrorCode;
import com.blur.userservice.identity.mapper.ProfileMapper;
import com.blur.userservice.identity.mapper.UserMapper;
import com.blur.userservice.identity.repository.RoleRepository;
import com.blur.userservice.identity.repository.UserRepository;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USER_LIST_CACHE_KEY = "users:all";
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    UserProfileService userProfileService; // LOCAL BEAN thay vi ProfileClient Feign
    ProfileMapper profileMapper;
    RoleRepository roleRepository;
    RedisTemplate<String, Object> redisTemplate;

    @Caching(
            evict = {
                    @CacheEvict(value = "users", allEntries = true),
                    @CacheEvict(value = "userById", key = "#result.id", condition = "#result != null")
            })
    @Transactional // Dam bao MySQL + Neo4j cung trong 1 transaction
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
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // LOCAL CALL - khong di HTTP, khong can Feign
        ProfileCreationRequest profileRequest = profileMapper.toProfileCreationRequest(request);
        profileRequest.setUsername(user.getUsername());
        profileRequest.setUserId(user.getId());
        profileRequest.setEmail(user.getEmail());
        userProfileService.createProfile(profileRequest);

        var userResponse = userMapper.toUserResponse(user);
        return userResponse;
    }

    public void createPassword(UserCreationPasswordRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (StringUtils.hasText(user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_EXISTED);
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", unless = "#result == null || #result.isEmpty()")
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @Cacheable(value = "userById", key = "#userId", unless = "#result == null")
    public User getUserById(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
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
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));
        return userResponse;
    }
}