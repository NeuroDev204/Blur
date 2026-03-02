package com.blur.profileservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.blur.profileservice.configuration.RedisConfig;
import com.blur.profileservice.dto.response.ApiResponse;
import com.blur.profileservice.dto.response.RecommendationPageResponse;
import com.blur.profileservice.service.UserProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationController {

  UserProfileService userProfileService;


  // goi t dua tren ket noi chung
  @GetMapping("/mutual")
  public ApiResponse<RecommendationPageResponse> getMutualRecommendatApiResponse(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    RecommendationPageResponse result = userProfileService.getMutualRecommendations(page, size);
    return ApiResponse.<RecommendationPageResponse>builder()
        .result(result)
        .build();
  }

  // goi y cung thanh pho
  @GetMapping("/nearby")
  public ApiResponse<RecommendationPageResponse> getSameCityRecommendations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    RecommendationPageResponse result = userProfileService.getSameCityRecommendations(page, size);
    return ApiResponse.<RecommendationPageResponse>builder()
        .result(result)
        .build();
  }

  // goi y dua tren so thich tuong tu
  @GetMapping("/similar")
  public ApiResponse<RecommendationPageResponse> getMimilarRecommendations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    RecommendationPageResponse result = userProfileService.getSimilarTasteRecommendations(page, size);
    return ApiResponse.<RecommendationPageResponse>builder()
        .result(result)
        .build();
  }

  // goi y nguoi dung pho bien
  @GetMapping("/popular")
  public ApiResponse<RecommendationPageResponse> getPopularRecommendations(
      @RequestParam(defaultValue = "100") int minFollowers,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    var result = userProfileService.getPopularRecommendations(minFollowers, page, size);
    return ApiResponse.<RecommendationPageResponse>builder()
        .result(result)
        .build();
  }
}
