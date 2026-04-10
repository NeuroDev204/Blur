package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserProfileController {
  UserProfileService userProfileService;
  UserProfileRepository userProfileRepository;
  UserProfileMapper userProfileMapper;

  @PostMapping({ "/internal/users", "/profile/internal/users" })
  public ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest request) {
    var result = userProfileService.createProfile(request);
    result.setCreatedAt(LocalDate.now());
    return ApiResponse.<UserProfileResponse>builder()
        .code(1000)
        .result(result)
        .build();
  }

  @GetMapping({ "/internal/users/{userId}", "/profile/internal/users/{userId}" })
  public ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId) {
    return ApiResponse.<UserProfileResponse>builder()
        .result(userProfileService.getByUserId(userId))
        .build();
  }

  @GetMapping("/internal/users/{userId}/follower-ids")
  public ApiResponse<List<String>> getFollowerIds(@PathVariable String userId) {
    List<String> followerIds = userProfileService.getFollowerUserIds(userId);
    return ApiResponse.<List<String>>builder()
        .code(1000)
        .result(followerIds)
        .build();
  }

  /**
   * Lấy tất cả profiles (dùng cho test data generation)
   */
  @GetMapping("/internal/users/all")
  public ApiResponse<List<UserProfileResponse>> getAllProfiles() {
    List<UserProfile> profiles = userProfileRepository.findAll();
    List<UserProfileResponse> result = profiles.stream()
        .map(userProfileMapper::toUserProfileResponse)
        .collect(Collectors.toList());
    return ApiResponse.<List<UserProfileResponse>>builder()
        .code(1000)
        .result(result)
        .build();
  }

  /**
   * Follow user internal (không cần authentication - chỉ dùng cho test)
   */
  @PostMapping("/internal/users/follow")
  public ApiResponse<String> followUserInternal(
      @RequestParam("fromProfileId") String fromProfileId,
      @RequestParam("toProfileId") String toProfileId) {
    try {
      userProfileRepository.follow(fromProfileId, toProfileId);
      return ApiResponse.<String>builder()
          .code(1000)
          .result("Followed successfully")
          .build();
    } catch (Exception e) {
      return ApiResponse.<String>builder()
          .code(5000)
          .message("Failed to follow: " + e.getMessage())
          .build();
    }
  }

  /**
   * ====================================================
   * ENDPOINT CHÍNH: Tạo random follows cho tất cả users
   * ====================================================
   * <p>
   * Sử dụng trong Postman:
   * POST
   * http://localhost:8082/profile/internal/generate-follows?minFollows=5&maxFollows=50
   */
  @PostMapping("/internal/generate-follows")
  public ApiResponse<Map<String, Object>> generateRandomFollows(
      @RequestParam(defaultValue = "5") int minFollows,
      @RequestParam(defaultValue = "50") int maxFollows) {

    long startTime = System.currentTimeMillis();
    Map<String, Object> result = new HashMap<>();

    try {
      // CHỈ lấy danh sách IDs - tránh load full entity + relationships
      List<String> allProfileIds = userProfileRepository.findAllProfileIds();
      int profileCount = allProfileIds.size();

      if (profileCount < 2) {
        result.put("error", "Need at least 2 profiles to create follows");
        return ApiResponse.<Map<String, Object>>builder()
            .code(4000)
            .message("Not enough profiles")
            .result(result)
            .build();
      }

      Random random = new Random();
      AtomicInteger totalFollows = new AtomicInteger(0);
      AtomicInteger failedFollows = new AtomicInteger(0);

      for (int i = 0; i < profileCount; i++) {
        String fromId = allProfileIds.get(i);
        int followCount = random.nextInt(maxFollows - minFollows + 1) + minFollows;

        // Chọn ngẫu nhiên người để follow
        Set<Integer> followedIndices = new HashSet<>();
        while (followedIndices.size() < followCount && followedIndices.size() < profileCount - 1) {
          int targetIndex = random.nextInt(profileCount);
          if (targetIndex != i) {
            followedIndices.add(targetIndex);
          }
        }

        // Tạo follow relationships bằng Cypher query (chỉ dùng ID)
        for (int targetIndex : followedIndices) {
          String toId = allProfileIds.get(targetIndex);
          try {
            userProfileRepository.follow(fromId, toId);
            totalFollows.incrementAndGet();
          } catch (Exception e) {
            failedFollows.incrementAndGet();
          }
        }

      }

      // Cập nhật follow counts cho tất cả users
      long updatedCount = userProfileRepository.updateAllFollowCounts();

      long duration = System.currentTimeMillis() - startTime;

      result.put("totalProfiles", profileCount);
      result.put("totalFollowsCreated", totalFollows.get());
      result.put("failedFollows", failedFollows.get());
      result.put("followCountsUpdated", updatedCount);
      result.put("durationMs", duration);
      result.put("durationFormatted", formatDuration(duration));
      result.put("avgFollowsPerUser", (double) totalFollows.get() / profileCount);

      return ApiResponse.<Map<String, Object>>builder()
          .code(1000)
          .message("Random follows generated successfully!")
          .result(result)
          .build();

    } catch (Exception e) {
      result.put("error", e.getMessage());
      return ApiResponse.<Map<String, Object>>builder()
          .code(5000)
          .message("Error: " + e.getMessage())
          .result(result)
          .build();
    }
  }

  /**
   * Set random city cho tất cả users
   * <p>
   * POST http://localhost:8082/profile/internal/generate-cities
   */
  @PostMapping("/internal/generate-cities")
  public ApiResponse<Map<String, Object>> generateRandomCities() {
    long startTime = System.currentTimeMillis();
    Map<String, Object> result = new HashMap<>();

    String[] CITIES = {
        "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
        "Biên Hòa", "Nha Trang", "Huế", "Buôn Ma Thuột", "Quy Nhơn",
        "Vũng Tàu", "Đà Lạt", "Long Xuyên", "Thái Nguyên", "Nam Định",
        "Vinh", "Thanh Hóa", "Bắc Ninh"
    };

    try {
      // CHỈ lấy IDs - tránh load full entity
      List<String> allProfileIds = userProfileRepository.findAllProfileIds();
      Random random = new Random();
      int updated = 0;

      for (String profileId : allProfileIds) {
        // Dùng Cypher query trực tiếp, KHÔNG load entity
        userProfileRepository.setCity(profileId, CITIES[random.nextInt(CITIES.length)]);
        updated++;

        if (updated % 500 == 0) {
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      result.put("totalUpdated", updated);
      result.put("durationMs", duration);
      result.put("durationFormatted", formatDuration(duration));

      return ApiResponse.<Map<String, Object>>builder()
          .code(1000)
          .message("Random cities assigned successfully!")
          .result(result)
          .build();

    } catch (Exception e) {
      result.put("error", e.getMessage());
      return ApiResponse.<Map<String, Object>>builder()
          .code(5000)
          .message("Error: " + e.getMessage())
          .result(result)
          .build();
    }
  }

  private String formatDuration(long ms) {
    long seconds = ms / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;
    if (minutes > 0) {
      return String.format("%d min %d sec", minutes, seconds);
    }
    return String.format("%d sec %d ms", seconds, ms % 1000);
  }
}
