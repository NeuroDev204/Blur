package com.blur.profileservice.service;

import com.blur.profileservice.dto.event.Event;
import com.blur.profileservice.dto.request.ProfileCreationRequest;
import com.blur.profileservice.dto.request.UserProfileUpdateRequest;
import com.blur.profileservice.dto.response.RecommendationPageResponse;
import com.blur.profileservice.dto.response.RecommendationResponse;
import com.blur.profileservice.dto.response.UserProfileResponse;
import com.blur.profileservice.entity.UserProfile;
import com.blur.profileservice.enums.RecommendationType;
import com.blur.profileservice.exception.AppException;
import com.blur.profileservice.exception.ErrorCode;
import com.blur.profileservice.mapper.UserProfileMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileService {
  UserProfileRepository userProfileRepository;
  UserProfileMapper userProfileMapper;
  NotificationClient notificationClient;


  @Caching(
      evict = {
          @CacheEvict(value = "profiles", allEntries = true),
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
      throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
    }
    return userProfileMapper.toUserProfileResponse(userProfile);
  }

  @Cacheable(value = "profiles", key = "#id", unless = "#result == null ")
  public UserProfile getUserProfile(String id) {
    return userProfileRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
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
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
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
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

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
      throw new AppException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
    }

    // Lấy Neo4j UUID từ userId
    var requester = userProfileRepository.findUserProfileByUserId(reqUserId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

    var followingUser = userProfileRepository.findUserProfileById(followerId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    userProfileRepository.follow(requester.getId(), followerId);
    log.info("following: {}", followingUser);

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
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

    var followingUser = userProfileRepository.findUserProfileById(followerId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
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
  public List<UserProfileResponse> search(String request) {
    var userId = SecurityContextHolder.getContext().getAuthentication().getName();
    List<UserProfile> userProfiles = userProfileRepository.findAllByUsernameLike(request);
    return userProfiles.stream()
        .filter(userProfile -> !userId.equals(userProfile.getUserId()))
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }


  // lay goi y dua tren ket noi chung
  // cache 10 phut
  @Cacheable(
      value = "recommendation:mutual",
      key = "#root.target.getCurrentUserProfileId",
      unless = "#result.content.isEmpty()"
  )
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

  // lay goi y dua tren so thich tuong tu (follow cung nguoi)
  @Cacheable(
      value = "recommendations:taste",
      key = "#root.tartget.getCurrentProfileId() +'-'+#page+'-'+#size",
      unless = "#result.content.isEmpty()"
  )
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

  // lay goi y nguoi dung cung thanh pho
  @Cacheable(
  	value = "recommnedations:city",
  	 key = "#root.target.getCurrentProfileId() + '-' + #page + '-' + #size",
  	unless = "#result.content.isEmpty()"
	)
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
  // lay goi y nguoi dung pho bien (nhieu follower)
  @Cacheable(
      value = "recommedations:popular",
      key = "#root.target.getCurrentProfileId() +'-'+ #minFollowers +'-' + #page+'-'+#size",
      unless = "#result.content.isEmpty()"
  )
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

  // lay goi y da yeu to
  // uu tien ket noi chung > cung thanh pho > so thich > pho bien
  @Cacheable(
      value = "recommendations:combined",
      key = "#root.target.getCurrentProfileId() + '-' + #limit",
      unless = "#result.isEmpty()"
  )
  @Transactional(readOnly = true)
  public List<RecommendationResponse> getCombinedRecommendations(int limit) {
    String userId = getCurrentProfileId();
    // su dung set de giu thu tu va loai bo trung lap
    Set<String> addedIds = new HashSet<>();
    List<RecommendationResponse> combined = new ArrayList<>();
    // uu tien cao nhat: ket noi chung (40%
    int mutualLimit = (int) Math.ceil(limit * 0.4);
    List<UserProfile> mutualRecs = userProfileRepository.findMutualRecommendations(userId, 0, mutualLimit);
    for (UserProfile profile : mutualRecs) {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.MUTUAL));
      }
    }
    // 2 cung thanh pho (25%)
    int cityLimit = (int) Math.ceil(limit * 0.25);
    List<UserProfile> cityRecs = userProfileRepository
        .findSameCityRecommendations(userId, 0, cityLimit);
    cityRecs.forEach(profile -> {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.SAME_CITY));
      }
    });
    // so thich tuong tu (25%)
    int tasteLimit = (int) Math.ceil(limit * 0.25);
    List<UserProfile> tasteRecs = userProfileRepository
        .findSimilarTasteRecommendations(userId, 0, tasteLimit);
    tasteRecs.forEach(profile -> {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.SIMILAR_TASTE));
      }
    });
    // nguoi dung pho bien (10%
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

  // lay goi y nhanh cho sidebar
  @Cacheable(
      value = "recommendations:quick",
      key = "#root.target.getCurrentProfileId() + '-' + #limit",
      unless = "#result.isEmpty()"
  )
  @Transactional(readOnly = true)
  public List<RecommendationResponse> getQuickRecommendations(int limit) {
    String userId = getCurrentProfileId();
    List<UserProfile> recommendations = userProfileRepository.findCombinedRecommendations(userId, 0, limit);
    return recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.COMBINED))
        .collect(Collectors.toList());
  }

  // kiem tra da follow chua
  @Transactional(readOnly = true)
  public boolean isFollowing(String targetUserId) {
    String currentUserId = getCurrentProfileId();
    return userProfileRepository.isFollowing(currentUserId, targetUserId);
  }

  //Vo hieu hoa cache khi co thay doi follow
  @CacheEvict(
      value = {
          "recommendations:mutual",
          "recommendations:taste",
          "recommnedation:city",
          "recommendations:popular",
          "recommendations:combined",
          "recommendations:quick",
      },
      allEntries = true
  )
  public void invalidateRecommnedationCache() {
    log.info("invalidating all recommendation caches");
  }

  // cap nhat follow counts
  @Transactional
  public void updateFollowCounts(String userId) {
    userProfileRepository.updateAllFollowCounts();
  }

  // backfill tat ca follow counts
  @Transactional
  public long backFillAllFollowCounts() {
    long updated = userProfileRepository.updateAllFollowCounts();
    return updated;
  }

  public String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  public String getUserIdByProfileId(String profileId) {
    return userProfileRepository.findById(profileId)
        .map(UserProfile::getUserId)
        .orElse(null);
  }

  /**
   * Lấy profile ID của user hiện tại từ SecurityContext
   */
  public String getCurrentProfileId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    String userId = authentication.getName();

    return userProfileRepository.findByUserId(userId)
        .map(UserProfile::getId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
  }

  /**
   * Map UserProfile entity sang RecommendationResponse DTO
   */
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
        .mutualConnections(0) // Sẽ được tính riêng nếu cần
        .recommendationType(type)
        .build();
  }

  /**
   * Build response phân trang
   */
  private RecommendationPageResponse buildPageResponse(
      List<RecommendationResponse> content,
      int page,
      int size,
      long total
  ) {
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
