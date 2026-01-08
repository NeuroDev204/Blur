package com.blur.profileservice.service;

import com.blur.common.dto.response.UserProfileResponse;
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.blur.profileservice.dto.event.Event;
import com.blur.profileservice.dto.request.ProfileCreationRequest;
import com.blur.profileservice.dto.request.UserProfileUpdateRequest;
import com.blur.profileservice.entity.UserProfile;
import com.blur.profileservice.repository.UserProfileRepository;
import com.blur.profileservice.repository.httpclient.NotificationClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileService {
    UserProfileRepository userProfileRepository;
    com.blur.profileservice.mapper.userProfileMapper userProfileMapper;
    NotificationClient notificationClient;


    @Caching(
            evict = {
                    @CacheEvict(value = "profiles" , allEntries = true),
                    @CacheEvict(value = "profileByUserId", key = "#request.userId")
            }
    )
    public UserProfileResponse createProfile(ProfileCreationRequest request) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile.setUsername(request.getUsername());
        userProfile.setCreatedAt(LocalDate.now());
        userProfile.setEmail(request.getEmail());
        try {
            userProfile = userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    @Cacheable(value = "profiles", key = "#id", unless = "#result == null ")
    public UserProfile getUserProfile(String id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }

    @Cacheable(
            value = "searchResults",
            key = "'firstName-' + #firstName",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileResponse> findUserProfileByFirstName(String firstName) {
        return userProfileRepository.findAllByFirstNameContainingIgnoreCase(firstName)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(
            value = "profiles",
            key = "'all'",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileResponse> getAllUserProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Cacheable(value = "profileByUserId", key = "#userId", unless = "#result == null")
    public UserProfileResponse getByUserId(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_NOT_EXIST));
        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    @Cacheable(
            value = "myProfile",
            key = "#root.target.getCurrentUserId()",
            unless = "#result == null "
    )
    public UserProfileResponse myProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    @Caching(
            put = @CachePut(value = "profiles", key = "#userProfileId"),
            evict = {
                    @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
                    @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
                    @CacheEvict(value = "searchResults", allEntries = true)
            }
    )
    public UserProfile updateUserProfile(String userProfileId, UserProfileUpdateRequest request) {
        UserProfile userProfile = getUserProfile(userProfileId);
        userProfileMapper.updateUserProfile(userProfile, request);
        return userProfileRepository.save(userProfile);
    }

    @Caching(evict = {
            @CacheEvict(value = "profiles", key = "#userProfileId"),
            @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
            @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
            @CacheEvict(value = "followers", allEntries = true),
            @CacheEvict(value = "following", allEntries = true),
            @CacheEvict(value = "searchResults", allEntries = true)
    })
    public void deleteUserProfile(String userProfileId) {
        userProfileRepository.deleteById(userProfileId);
    }

    @Caching(evict = {
            @CacheEvict(value = "followers", key = "#followerId"),
            @CacheEvict(value = "following", key = "#root.target.getCurrentUserId()")
    })
    public String followUser(String followerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String reqUserId = authentication.getName();

        if (reqUserId.equals(followerId)) {
            throw new BlurException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        // Lấy Neo4j UUID từ userId
        var requester = userProfileRepository.findUserProfileByUserId(reqUserId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));

        var followingUser = userProfileRepository.findUserProfileById(followerId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));
        userProfileRepository.follow(requester.getId(), followerId);
        log.info("following: {}" , followingUser);

        // gui notification
        Event event = Event.builder()
                .senderId(requester.getId())
                .senderName(requester.getFirstName() + " " + requester.getLastName())
                .receiverId(followingUser.getId())
                .receiverName(followingUser.getFirstName() + " " + followingUser.getLastName())
                .receiverEmail(followingUser.getEmail())
                .timestamp(LocalDateTime.now())
                .build();
        log.info("Sending follow event: {}", event);
        notificationClient.sendFollowNotification(event);


        return "You are following " + followingUser.getFirstName();
    }

    @Caching(evict = {
            @CacheEvict(value = "followers", key = "#followerId"),
            @CacheEvict(value = "following", key = "#root.target.getCurrentUserId()")
    })
    public String unfollowUser(String followerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String reqUserId = authentication.getName();

        var requester = userProfileRepository.findUserProfileByUserId(reqUserId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));

        var followingUser = userProfileRepository.findUserProfileById(followerId)
                .orElseThrow(() -> new BlurException(ErrorCode.USER_PROFILE_NOT_FOUND));
        requester.getFollowers().remove(followingUser);
        userProfileRepository.unfollow(requester.getId(), followerId);

        return "You unfollowed " + followingUser.getFirstName();
    }
    @Cacheable(
            value = "followers",
            key = "#profileId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileResponse> getFollowers(String profileId) {
        return userProfileRepository.findAllFollowersById(profileId)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
    @Cacheable(
            value = "following",
            key = "#profileId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileResponse> getFollowing(String profileId) {
        return userProfileRepository.findAllFollowingById(profileId)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }



    @Cacheable(
            value = "searchResults",
            key = "'username-' + #request",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileResponse> search(String request){
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserProfile>  userProfiles = userProfileRepository.findAllByUsernameLike(request);
        return  userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public String getUserIdByProfileId(String profileId) {
        return userProfileRepository.findById(profileId)
                .map(UserProfile::getUserId)
                .orElse(null);
    }
}
