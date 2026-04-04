package com.blur.userservice.profile.service;

import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.dto.request.UserProfileUpdateRequest;
import com.blur.userservice.profile.dto.response.RecommendationPageResponse;
import com.blur.userservice.profile.dto.response.RecommendationResponse;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.enums.RecommendationType;
import com.blur.userservice.profile.exception.AppException;
import com.blur.userservice.profile.exception.ErrorCode;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.repository.httpclient.ContentServiceClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {
    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    ContentServiceClient contentServiceClient;

    @Caching(evict = {
            @CacheEvict(value = "profileByUserId", key = "#request.userId")
    })
    public UserProfileResponse createProfile(ProfileCreationRequest request) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile.setUserId(request.getUserId());
        userProfile.setUsername(request.getUsername());
        userProfile.setCreatedAt(LocalDate.now());
        userProfile.setEmail(request.getEmail());
        try {
            userProfile = userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfile getUserProfile(String id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }

    @Cacheable(value = "searchResults", key = "'firstName-' + #firstName", unless = "#result == null || #result.isEmpty()")
    public List<UserProfileResponse> findUserProfileByFirstName(String firstName) {
        return userProfileRepository.findAllByFirstNameContainingIgnoreCase(firstName)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "profiles", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<UserProfileResponse> getAllUserProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Cacheable(value = "profileByUserId", key = "#userId", sync = true)
    public UserProfileResponse getByUserId(String userId) {
        return userProfileMapper.toUserProfileResponse(getOrCreateProfileByUserId(userId));
    }

    @Cacheable(value = "myProfile", key = "#root.target.getCurrentUserId()",
            condition = "T(org.springframework.util.StringUtils).hasText(#root.target.getCurrentUserId())",
            unless = "#result == null")
    public UserProfileResponse myProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        if (!org.springframework.util.StringUtils.hasText(userId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userProfileMapper.toUserProfileResponse(getOrCreateProfileByUserId(userId));
    }

    @Caching(evict = {
            @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
            @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
            @CacheEvict(value = "searchResults", allEntries = true)
    })
    public UserProfileResponse updateUserProfile(String userProfileId, UserProfileUpdateRequest request) {
        UserProfile userProfile = getUserProfile(userProfileId);
        userProfileMapper.updateUserProfile(userProfile, request);
        UserProfile saved = userProfileRepository.save(userProfile);
        return userProfileMapper.toUserProfileResponse(saved);
    }

    @Caching(evict = {
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
            @CacheEvict(value = "followers", allEntries = true),
            @CacheEvict(value = "following", allEntries = true),
            @CacheEvict(value = "recommendations:mutual", allEntries = true),
            @CacheEvict(value = "recommendations:taste", allEntries = true),
            @CacheEvict(value = "recommendations:city", allEntries = true),
            @CacheEvict(value = "recommendations:popular", allEntries = true),
            @CacheEvict(value = "recommendations:combined", allEntries = true),
            @CacheEvict(value = "recommendations:quick", allEntries = true)
    })
    public String followUser(String followerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String reqUserId = authentication.getName();

        if (reqUserId.equals(followerId)) {
            throw new AppException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        var requester = getOrCreateProfileByUserId(reqUserId);

        var followingUser = userProfileRepository.findByUserId(followerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        userProfileRepository.follow(requester.getId(), followingUser.getId());
        userProfileRepository.updateFollowCounts(requester.getId());
        userProfileRepository.updateFollowCounts(followingUser.getId());

        // Backfill feed: tạo feed items cho bài viết cũ của người được follow
        try {
            contentServiceClient.backfillFeed(reqUserId, followerId);
        } catch (Exception e) {
            // non-blocking: feed backfill thất bại không ảnh hưởng follow
        }

        return "You are following " + followingUser.getFirstName();
    }

    @Caching(evict = {
            @CacheEvict(value = "followers", allEntries = true),
            @CacheEvict(value = "following", allEntries = true),
            @CacheEvict(value = "recommendations:mutual", allEntries = true),
            @CacheEvict(value = "recommendations:taste", allEntries = true),
            @CacheEvict(value = "recommendations:city", allEntries = true),
            @CacheEvict(value = "recommendations:popular", allEntries = true),
            @CacheEvict(value = "recommendations:combined", allEntries = true),
            @CacheEvict(value = "recommendations:quick", allEntries = true)
    })
    public String unfollowUser(String followerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String reqUserId = authentication.getName();

        var requester = getOrCreateProfileByUserId(reqUserId);

        var followingUser = userProfileRepository.findByUserId(followerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        requester.getFollowers().remove(followingUser);
        userProfileRepository.unfollow(requester.getId(), followingUser.getId());
        userProfileRepository.updateFollowCounts(requester.getId());
        userProfileRepository.updateFollowCounts(followingUser.getId());

        return "You unfollowed " + followingUser.getFirstName();
    }

    @Cacheable(value = "followers", key = "#profileId", unless = "#result == null || #result.isEmpty()")
    public List<UserProfileResponse> getFollowers(String profileId) {
        return userProfileRepository.findAllFollowersById(profileId)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Cacheable(value = "following", key = "#profileId", unless = "#result == null || #result.isEmpty()")
    public List<UserProfileResponse> getFollowing(String profileId) {
        return userProfileRepository.findAllFollowingById(profileId)
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Cacheable(value = "searchResults", key = "'username-' + #request", unless = "#result == null || #result.isEmpty()")
    public List<UserProfileResponse> search(String request) {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserProfile> userProfiles = userProfileRepository.findAllByUsernameLike(request);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Cacheable(value = "recommendations:mutual", key = "#root.target.getCurrentProfileId() + '-' + #page + '-' + #size", sync = true)
    @Transactional(readOnly = true)
    public RecommendationPageResponse getMutualRecommendations(int page, int size) {
        String userId = getCurrentProfileId();
        int skip = page * size;
        List<UserProfile> recommendations = userProfileRepository.findMutualRecommendations(userId, skip, size);
        long total = userProfileRepository.countMutualRecommendations(userId);
        List<RecommendationResponse> content = recommendations.stream()
                .map(profile -> mapToResponse(profile, RecommendationType.MUTUAL))
                .collect(Collectors.toList());
        return buildPageResponse(content, page, size, total);
    }

    @Cacheable(value = "recommendations:taste", key = "#root.target.getCurrentProfileId() + '-' + #page + '-' + #size", sync = true)
    @Transactional(readOnly = true)
    public RecommendationPageResponse getSimilarTasteRecommendations(int page, int size) {
        String userId = getCurrentProfileId();
        int skip = page * size;
        List<UserProfile> recommendations = userProfileRepository
                .findSimilarTasteRecommendations(userId, skip, size);
        long total = userProfileRepository.countSimilarTasteRecommendations(userId);
        List<RecommendationResponse> content = recommendations.stream()
                .map(profile -> mapToResponse(profile, RecommendationType.SAME_CITY))
                .collect(Collectors.toList());
        return buildPageResponse(content, page, size, total);
    }

    @Cacheable(value = "recommendations:city", key = "#root.target.getCurrentProfileId() + '-' + #page + '-' + #size", sync = true)
    @Transactional(readOnly = true)
    public RecommendationPageResponse getSameCityRecommendations(int page, int size) {
        String userId = getCurrentProfileId();
        int skip = page * size;
        List<UserProfile> recommendations = userProfileRepository
                .findSameCityRecommendations(userId, skip, size);
        long total = userProfileRepository.countSameCityRecommendations(userId);
        List<RecommendationResponse> content = recommendations.stream()
                .map(profile -> mapToResponse(profile, RecommendationType.SAME_CITY))
                .collect(Collectors.toList());
        return buildPageResponse(content, page, size, total);
    }

    @Cacheable(value = "recommendations:popular", key = "#root.target.getCurrentProfileId() +'-'+ #minFollowers +'-' + #page+'-'+#size", sync = true)
    @Transactional(readOnly = true)
    public RecommendationPageResponse getPopularRecommendations(int minFollowers, int page, int size) {
        String userId = getCurrentProfileId();
        int skip = page * size;
        List<UserProfile> recommendations = userProfileRepository
                .findPopularRecommendations(userId, minFollowers, skip, size);
        long total = userProfileRepository.countPopularRecommendations(userId, minFollowers);
        List<RecommendationResponse> content = recommendations.stream()
                .map(profile -> mapToResponse(profile, RecommendationType.POPULAR))
                .collect(Collectors.toList());
        return buildPageResponse(content, page, size, total);
    }

    @Cacheable(value = "recommendations:combined", key = "#root.target.getCurrentProfileId() + '-' + #limit", sync = true)
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getCombinedRecommendations(int limit) {
        String userId = getCurrentProfileId();
        Set<String> addedIds = new HashSet<>();
        List<RecommendationResponse> combined = new ArrayList<>();
        int mutualLimit = (int) Math.ceil(limit * 0.4);
        List<UserProfile> mutualRecs = userProfileRepository.findMutualRecommendations(userId, 0, mutualLimit);
        for (UserProfile profile : mutualRecs) {
            if (addedIds.add(profile.getId())) {
                combined.add(mapToResponse(profile, RecommendationType.MUTUAL));
            }
        }
        int cityLimit = (int) Math.ceil(limit * 0.25);
        List<UserProfile> cityRecs = userProfileRepository
                .findSameCityRecommendations(userId, 0, cityLimit);
        cityRecs.forEach(profile -> {
            if (addedIds.add(profile.getId())) {
                combined.add(mapToResponse(profile, RecommendationType.SAME_CITY));
            }
        });
        int tasteLimit = (int) Math.ceil(limit * 0.25);
        List<UserProfile> tasteRecs = userProfileRepository
                .findSimilarTasteRecommendations(userId, 0, tasteLimit);
        tasteRecs.forEach(profile -> {
            if (addedIds.add(profile.getId())) {
                combined.add(mapToResponse(profile, RecommendationType.SIMILAR_TASTE));
            }
        });
        int popularLimit = (int) Math.ceil(limit * 0.1);
        List<UserProfile> popularRecs = userProfileRepository
                .findPopularRecommendations(userId, 100, 0, popularLimit);
        popularRecs.forEach(profile -> {
            if (addedIds.add(profile.getId())) {
                combined.add(mapToResponse(profile, RecommendationType.POPULAR));
            }
        });
        return combined.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "recommendations:quick", key = "#root.target.getCurrentProfileId() + '-' + #limit", sync = true)
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getQuickRecommendations(int limit) {
        String userId = getCurrentProfileId();
        List<UserProfile> recommendations = userProfileRepository.findCombinedRecommendations(userId, 0, limit);
        return recommendations.stream()
                .map(profile -> mapToResponse(profile, RecommendationType.COMBINED))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String targetUserId) {
        String currentUserId = getCurrentProfileId();
        return userProfileRepository.isFollowing(currentUserId, targetUserId);
    }

    @CacheEvict(value = {
            "recommendations:mutual",
            "recommendations:taste",
            "recommendations:city",
            "recommendations:popular",
            "recommendations:combined",
            "recommendations:quick",
    }, allEntries = true)
    public void invalidateRecommendationCache() {
    }

    @Transactional
    public void updateFollowCounts(String userId) {
        userProfileRepository.updateAllFollowCounts();
    }

    @Transactional
    public long backFillAllFollowCounts() {
        long updated = userProfileRepository.updateAllFollowCounts();
        return updated;
    }

    public List<String> getFollowerUserIds(String userId) {
        return userProfileRepository.findFollowerUserIdsByUserId(userId);
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public String getUserIdByProfileId(String profileId) {
        return userProfileRepository.findById(profileId)
                .map(UserProfile::getUserId)
                .orElse(null);
    }

    public String getCurrentProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userId = authentication.getName();
        if (!org.springframework.util.StringUtils.hasText(userId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return getOrCreateProfileByUserId(userId).getId();
    }

    private UserProfile getOrCreateProfileByUserId(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private RecommendationResponse mapToResponse(UserProfile profile, RecommendationType type) {
        return RecommendationResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .imageUrl(profile.getImageUrl())
                .bio(profile.getBio())
                .city(profile.getCity())
                .followerCount(profile.getFollowersCount() != null ? profile.getFollowersCount() : 0)
                .followingCount(profile.getFollowingCount() != null ? profile.getFollowingCount() : 0)
                .mutualConnections(0)
                .recommendationType(type)
                .build();
    }

    private RecommendationPageResponse buildPageResponse(
            List<RecommendationResponse> content,
            int page,
            int size,
            long total) {
        int totalPages = (int) Math.ceil((double) total / size);

        return RecommendationPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}