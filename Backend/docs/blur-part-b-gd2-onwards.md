
---

## GĐ 2: ASYNC AI MODERATION PIPELINE (CONTENT SERVICE - JAVA SIDE)

> **Mục tiêu:** Tích hợp content-service (Java) với model-service (Python) qua Kafka.
> Khi user post comment → lưu PENDING_MODERATION → gửi Kafka → nhận kết quả → update.

### 2.1 Tổng quan luồng

```
User → POST /post/comments → CommentService
                                  │
                                  ├─ 1. Save comment (PENDING_MODERATION)
                                  ├─ 2. Return 202 Accepted
                                  └─ 3. ModerationProducer.submit() → Kafka
                                                                        │
                              Kafka: comment-moderation-requests        │
                                                                        ▼
                                                                Model Service
                                                                  PhoBERT
                                                                        │
                              Kafka: comment-moderation-results         │
                                                                        ▼
                         ModerationResultConsumer.consume() ← content-service
                                  │
                                  ├─ Update comment.status = APPROVED/REJECTED
                                  ├─ Set comment.moderationConfidence
                                  └─ Set comment.moderatedAt
```

### 2.2 Thêm field vào Comment Entity

**File:** `content-service/src/.../post/entity/Comment.java`

```java
package com.blur.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("comments")
public class Comment {
    @Id
    String id;
    String postId;
    String userId;
    String username;
    String userAvatar;
    String content;
    LocalDateTime createdDate;

    // ==================== THÊM MỚI CHO MODERATION ====================
    String moderationStatus;           // PENDING_MODERATION, APPROVED, REJECTED
    Double moderationConfidence;       // 0.0 - 1.0
    String modelVersion;               // "phobert_toxic_FINAL"
    LocalDateTime moderatedAt;         // Timestamp khi model xử lý xong
}
```

### 2.3 ModerationStatus Enum

**File:** `content-service/src/.../post/enums/ModerationStatus.java`

```java
package com.blur.contentservice.post.enums;

public enum ModerationStatus {
    PENDING_MODERATION,  // Đang chờ model xử lý
    APPROVED,            // Comment an toàn
    REJECTED             // Comment toxic, ẩn khỏi feed
}
```

### 2.4 ModerationProducer

**File:** `content-service/src/.../kafka/ModerationProducer.java`

```java
package com.blur.contentservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Gửi comment mới tới Kafka để model-service chạy moderation.
 * Topic: comment-moderation-requests
 * Consumer: model-service (Python)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "comment-moderation-requests";

    /**
     * Gửi request moderation cho 1 comment.
     *
     * @param commentId ID comment trong MongoDB
     * @param postId    ID post chứa comment
     * @param userId    ID user tạo comment
     * @param content   Nội dung comment cần kiểm duyệt
     */
    public void submit(String commentId, String postId, String userId, String content) {
        try {
            var payload = Map.of(
                    "commentId", commentId,
                    "postId", postId,
                    "userId", userId,
                    "content", content
            );
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, commentId, json);
            log.info("Moderation request sent | commentId={}", commentId);
        } catch (Exception e) {
            log.error("Failed to send moderation request | commentId={}", commentId, e);
            // Không throw - comment vẫn ở trạng thái PENDING_MODERATION
            // Cronjob sẽ retry các comment PENDING quá lâu
        }
    }
}
```

### 2.5 ModerationResultConsumer

**File:** `content-service/src/.../kafka/ModerationResultConsumer.java`

```java
package com.blur.contentservice.kafka;

import com.blur.contentservice.post.repository.CommentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Nhận kết quả moderation từ model-service.
 * Topic: comment-moderation-results
 * Producer: model-service (Python)
 *
 * Flow:
 * 1. Nhận message JSON
 * 2. Parse commentId, status, confidence, toxicScore, modelVersion
 * 3. Tìm comment trong MongoDB
 * 4. Update trạng thái
 * 5. Nếu REJECTED → ẩn nội dung
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationResultConsumer {

    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;

    @KafkaListener(topics = "comment-moderation-results", groupId = "content-service")
    public void consume(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);

            String commentId = (String) result.get("commentId");
            String status = (String) result.get("status");
            double confidence = ((Number) result.get("confidence")).doubleValue();
            double toxicScore = ((Number) result.get("toxicScore")).doubleValue();
            String modelVersion = (String) result.get("modelVersion");

            commentRepository.findById(commentId).ifPresent(comment -> {
                comment.setModerationStatus(status);
                comment.setModerationConfidence(toxicScore);
                comment.setModelVersion(modelVersion);
                comment.setModeratedAt(LocalDateTime.now());

                if ("REJECTED".equals(status)) {
                    comment.setContent("[Bình luận đã bị ẩn bởi hệ thống kiểm duyệt]");
                }

                commentRepository.save(comment);
                log.info("Comment {} moderated → {} (confidence={}, toxicScore={})",
                        commentId, status, confidence, toxicScore);
            });
        } catch (Exception e) {
            log.error("Failed to process moderation result", e);
        }
    }
}
```

### 2.6 Sửa CommentService - Tích hợp Moderation

**File:** `content-service/src/.../post/service/CommentService.java`

Thêm inject và gọi ModerationProducer khi tạo comment:

```java
// THÊM import
import com.blur.contentservice.kafka.ModerationProducer;

// THÊM inject
private final ModerationProducer moderationProducer;

// SỬA method createComment
@Transactional
public CommentResponse createComment(String postId, CommentCreateRequest request) {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    var userProfile = profileClient.getProfile(userId).getResult();

    Comment comment = Comment.builder()
            .postId(postId)
            .userId(userId)
            .username(userProfile.getUsername())
            .userAvatar(userProfile.getImageUrl())
            .content(request.getContent())
            .createdDate(LocalDateTime.now())
            .moderationStatus("PENDING_MODERATION")  // <-- MỚI
            .build();

    comment = commentRepository.save(comment);

    // Gửi đi moderation async qua Kafka
    moderationProducer.submit(
            comment.getId(),
            postId,
            userId,
            request.getContent()
    );

    // Gửi notification
    notificationEventPublisher.publishCommentEvent(/* ... */);

    return commentMapper.toResponse(comment);
}
```

### 2.7 Kafka Config cho Content Service

**File:** `content-service/src/main/resources/application.yaml` (thêm section)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9093}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: content-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### 2.8 pom.xml dependency

```xml
<!-- Thêm vào content-service/pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 2.9 Checklist GĐ 2

- [ ] Thêm moderation fields vào Comment entity
- [ ] Tạo ModerationStatus enum
- [ ] Tạo ModerationProducer
- [ ] Tạo ModerationResultConsumer
- [ ] Sửa CommentService: set PENDING_MODERATION + gọi producer
- [ ] Thêm Kafka config vào application.yaml
- [ ] Thêm spring-kafka dependency
- [ ] Test E2E: post comment → Kafka → model-service → Kafka → update comment

---

## GĐ 3: NEO4J FOLLOW RECOMMENDATION

> **Đã triển khai chi tiết trong:** `neo4j-follow-recommendation.md`
> **Conversation tham khảo:** `2009e663` (Implementing Neo4j Recommendations)
>
> Dưới đây là code ĐẦY ĐỦ cho các file cần tạo/sửa trong profile-service.

### 3.1 Tổng quan hệ thống gợi ý

```
Thuật toán gợi ý (xếp theo ưu tiên):
1. Followers chung (Mutual Connections)     → Trọng số: 3
2. Sở thích tương tự (Similar Taste)       → Trọng số: 2
3. Cùng thành phố (Same City)              → Trọng số: 1
4. Người phổ biến gần đây (Popular/Active) → Trọng số: 1
5. Kết hợp đa yếu tố (Weighted Score)      → Production query
```

### 3.2 RecommendationType Enum

**File:** `profile-service/src/.../enums/RecommendationType.java`

```java
package com.blur.profileservice.enums;

public enum RecommendationType {
    MUTUAL,          // Có followers chung
    SIMILAR_TASTE,   // Follow cùng người
    SAME_CITY,       // Cùng thành phố
    POPULAR          // Phổ biến + hoạt động gần đây
}
```

### 3.3 RecommendationResponse DTO

**File:** `profile-service/src/.../dto/response/RecommendationResponse.java`

```java
package com.blur.profileservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationResponse {
    String id;
    String userId;
    String username;
    String firstName;
    String lastName;
    String imageUrl;
    String bio;
    String city;
    Integer followersCount;
    Integer followingCount;
    Integer mutualConnections;
    String recommendationType;    // MUTUAL, SIMILAR_TASTE, SAME_CITY, POPULAR
}
```

### 3.4 RecommendationPageResponse DTO

**File:** `profile-service/src/.../dto/response/RecommendationPageResponse.java`

```java
package com.blur.profileservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationPageResponse {
    List<RecommendationResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean hasNext;
    boolean hasPrevious;
}
```

### 3.5 MutualRecommendationProjection Interface

**File:** `profile-service/src/.../repository/MutualRecommendationProjection.java`

```java
package com.blur.profileservice.repository;

import java.util.List;

/**
 * Projection interface cho Cypher query trả về kết quả gợi ý.
 * Spring Data Neo4j tự động map các field từ RETURN clause
 * sang các getter tương ứng.
 */
public interface MutualRecommendationProjection {
    String getId();
    String getUserId();
    String getUsername();
    String getFirstName();
    String getLastName();
    String getImageUrl();
    String getBio();
    String getCity();
    Integer getFollowersCount();
    Integer getFollowingCount();
    Integer getMutualConnections();
    List<String> getMutualNames();
}
```

### 3.6 RecommendationRepository

**File:** `profile-service/src/.../repository/RecommendationRepository.java`

```java
package com.blur.profileservice.repository;

import com.blur.profileservice.entity.UserProfile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecommendationRepository extends Neo4jRepository<UserProfile, String> {

    // ========== 1. FOLLOWERS CHUNG ==========
    @Query("""
        MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
        MATCH (myFollowing)-[:follows]->(recommended:user_profile)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          AND NOT EXISTS((recommended)-[:BLOCKED]->(me))
        WITH recommended, COUNT(DISTINCT myFollowing) AS mutualCount
        ORDER BY mutualCount DESC
        SKIP $skip LIMIT $limit
        RETURN recommended
    """)
    List<UserProfile> findMutualRecommendations(
            @Param("userId") String userId,
            @Param("skip") int skip,
            @Param("limit") int limit);

    @Query("""
        MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
        MATCH (myFollowing)-[:follows]->(recommended:user_profile)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
        RETURN COUNT(DISTINCT recommended)
    """)
    long countMutualRecommendations(@Param("userId") String userId);

    @Query("""
        MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
        MATCH (myFollowing)-[:follows]->(recommended:user_profile)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
        WITH recommended,
             COUNT(DISTINCT myFollowing) AS mutualCount,
             COLLECT(DISTINCT myFollowing.firstName + ' ' + myFollowing.lastName)[0..3] AS mutualNames
        ORDER BY mutualCount DESC
        SKIP $skip LIMIT $limit
        RETURN recommended.id AS id,
               recommended.userId AS userId,
               recommended.username AS username,
               recommended.firstName AS firstName,
               recommended.lastName AS lastName,
               recommended.imageUrl AS imageUrl,
               recommended.bio AS bio,
               recommended.city AS city,
               recommended.followersCount AS followersCount,
               recommended.followingCount AS followingCount,
               mutualCount AS mutualConnections,
               mutualNames AS mutualNames
    """)
    List<MutualRecommendationProjection> findMutualRecommendationsWithDetails(
            @Param("userId") String userId,
            @Param("skip") int skip,
            @Param("limit") int limit);

    // ========== 2. SỞ THÍCH TƯƠNG TỰ ==========
    @Query("""
        MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
        MATCH (recommended:user_profile)-[:follows]->(following)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT (recommended)-[:follows]->(me)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
        WITH recommended, COUNT(DISTINCT following) AS sharedCount
        ORDER BY sharedCount DESC, recommended.followersCount DESC
        SKIP $skip LIMIT $limit
        RETURN recommended
    """)
    List<UserProfile> findSimilarTasteRecommendations(
            @Param("userId") String userId,
            @Param("skip") int skip,
            @Param("limit") int limit);

    @Query("""
        MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
        MATCH (recommended:user_profile)-[:follows]->(following)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT (recommended)-[:follows]->(me)
        RETURN COUNT(DISTINCT recommended)
    """)
    long countSimilarTasteRecommendations(@Param("userId") String userId);

    // ========== 3. CÙNG THÀNH PHỐ ==========
    @Query("""
        MATCH (me:user_profile {id: $userId})
        WHERE me.city IS NOT NULL AND me.city <> ''
        MATCH (recommended:user_profile)
        WHERE recommended.city = me.city
          AND recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
        RETURN recommended
        ORDER BY recommended.followersCount DESC
        SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findSameCityRecommendations(
            @Param("userId") String userId,
            @Param("skip") int skip,
            @Param("limit") int limit);

    @Query("""
        MATCH (me:user_profile {id: $userId})
        WHERE me.city IS NOT NULL AND me.city <> ''
        MATCH (recommended:user_profile)
        WHERE recommended.city = me.city
          AND recommended <> me
          AND NOT (me)-[:follows]->(recommended)
        RETURN COUNT(DISTINCT recommended)
    """)
    long countSameCityRecommendations(@Param("userId") String userId);

    // ========== 4. PHỔI BIẾN + HOẠT ĐỘNG ==========
    @Query("""
        MATCH (me:user_profile {id: $userId})
        MATCH (recommended:user_profile)
        WHERE recommended <> me
          AND NOT (me)-[:follows]->(recommended)
          AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          AND recommended.followersCount >= $minFollowers
        RETURN recommended
        ORDER BY recommended.followersCount DESC
        SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findPopularRecommendations(
            @Param("userId") String userId,
            @Param("minFollowers") int minFollowers,
            @Param("skip") int skip,
            @Param("limit") int limit);
}
```

### 3.7 RecommendationService

**File:** `profile-service/src/.../service/RecommendationService.java`

```java
package com.blur.profileservice.service;

import com.blur.profileservice.dto.response.RecommendationPageResponse;
import com.blur.profileservice.dto.response.RecommendationResponse;
import com.blur.profileservice.entity.UserProfile;
import com.blur.profileservice.enums.RecommendationType;
import com.blur.profileservice.repository.RecommendationRepository;
import com.blur.profileservice.repository.UserProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationService {

    RecommendationRepository recommendationRepository;
    UserProfileRepository userProfileRepository;

    /**
     * Lấy gợi ý theo loại cụ thể.
     */
    @Cacheable(value = "recommendations", key = "#type + ':' + #page + ':' + #size",
            unless = "#result.content.isEmpty()")
    public RecommendationPageResponse getRecommendations(
            RecommendationType type, int page, int size) {

        String userId = getCurrentUserId();
        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        int skip = page * size;
        List<UserProfile> recommendations;
        long totalElements;

        switch (type) {
            case MUTUAL -> {
                recommendations = recommendationRepository
                        .findMutualRecommendations(profile.getId(), skip, size);
                totalElements = recommendationRepository
                        .countMutualRecommendations(profile.getId());
            }
            case SIMILAR_TASTE -> {
                recommendations = recommendationRepository
                        .findSimilarTasteRecommendations(profile.getId(), skip, size);
                totalElements = recommendationRepository
                        .countSimilarTasteRecommendations(profile.getId());
            }
            case SAME_CITY -> {
                recommendations = recommendationRepository
                        .findSameCityRecommendations(profile.getId(), skip, size);
                totalElements = recommendationRepository
                        .countSameCityRecommendations(profile.getId());
            }
            case POPULAR -> {
                recommendations = recommendationRepository
                        .findPopularRecommendations(profile.getId(), 100, skip, size);
                totalElements = recommendations.size();
            }
            default -> {
                recommendations = List.of();
                totalElements = 0;
            }
        }

        List<RecommendationResponse> content = recommendations.stream()
                .map(rec -> toResponse(rec, type.name()))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return RecommendationPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    /**
     * Gợi ý kết hợp đa yếu tố (cho trang chính).
     * Ưu tiên: Mutual > Similar > SameCity > Popular
     */
    public List<RecommendationResponse> getMixedRecommendations(int limit) {
        String userId = getCurrentUserId();
        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        Set<String> seen = new HashSet<>();
        List<RecommendationResponse> results = new ArrayList<>();

        // Ưu tiên 1: Mutual (50% quota)
        addUnique(results, seen,
                recommendationRepository
                        .findMutualRecommendations(profile.getId(), 0, limit / 2),
                RecommendationType.MUTUAL.name());

        // Ưu tiên 2: Similar Taste (25% quota)
        addUnique(results, seen,
                recommendationRepository
                        .findSimilarTasteRecommendations(profile.getId(), 0, limit / 4),
                RecommendationType.SIMILAR_TASTE.name());

        // Ưu tiên 3: Same City (15% quota)
        addUnique(results, seen,
                recommendationRepository
                        .findSameCityRecommendations(profile.getId(), 0, limit / 6),
                RecommendationType.SAME_CITY.name());

        // Ưu tiên 4: Popular (còn lại)
        int remaining = limit - results.size();
        if (remaining > 0) {
            addUnique(results, seen,
                    recommendationRepository
                            .findPopularRecommendations(profile.getId(), 50, 0, remaining),
                    RecommendationType.POPULAR.name());
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private void addUnique(List<RecommendationResponse> results, Set<String> seen,
                           List<UserProfile> recs, String type) {
        for (UserProfile rec : recs) {
            if (seen.add(rec.getId())) {
                results.add(toResponse(rec, type));
            }
        }
    }

    private RecommendationResponse toResponse(UserProfile profile, String type) {
        return RecommendationResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .imageUrl(profile.getImageUrl())
                .bio(profile.getBio())
                .city(profile.getCity())
                .followersCount(profile.getFollowersCount())
                .followingCount(profile.getFollowingCount())
                .recommendationType(type)
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

### 3.8 RecommendationController

**File:** `profile-service/src/.../controller/RecommendationController.java`

```java
package com.blur.profileservice.controller;

import com.blur.profileservice.dto.response.ApiResponse;
import com.blur.profileservice.dto.response.RecommendationPageResponse;
import com.blur.profileservice.dto.response.RecommendationResponse;
import com.blur.profileservice.enums.RecommendationType;
import com.blur.profileservice.service.RecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile/users/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {

    RecommendationService recommendationService;

    /**
     * GET /profile/users/recommendations?type=MUTUAL&page=0&size=10
     * Lấy gợi ý theo loại cụ thể với phân trang.
     */
    @GetMapping
    public ApiResponse<RecommendationPageResponse> getRecommendations(
            @RequestParam(defaultValue = "MUTUAL") RecommendationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<RecommendationPageResponse>builder()
                .result(recommendationService.getRecommendations(type, page, size))
                .build();
    }

    /**
     * GET /profile/users/recommendations/mixed?limit=20
     * Lấy gợi ý kết hợp đa yếu tố cho trang chính (sidebar).
     */
    @GetMapping("/mixed")
    public ApiResponse<List<RecommendationResponse>> getMixedRecommendations(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.<List<RecommendationResponse>>builder()
                .result(recommendationService.getMixedRecommendations(limit))
                .build();
    }
}
```

### 3.9 Thêm property vào UserProfile entity

```java
// Thêm vào UserProfile.java
@Property("followersCount")
Integer followersCount = 0;

@Property("followingCount")
Integer followingCount = 0;

@Property("postCount")
Integer postCount = 0;

@Property("lastActiveAt")
LocalDateTime lastActiveAt;
```

### 3.10 Checklist GĐ 3

- [ ] Tạo RecommendationType enum
- [ ] Tạo RecommendationResponse + PageResponse DTOs
- [ ] Tạo MutualRecommendationProjection interface
- [ ] Tạo RecommendationRepository với 4 loại Cypher query
- [ ] Tạo RecommendationService (logic kết hợp + cache)
- [ ] Tạo RecommendationController (2 endpoints)
- [ ] Thêm property mới vào UserProfile entity
- [ ] Thêm Redis cache config cho recommendations
- [ ] Test: GET /profile/users/recommendations?type=MUTUAL
- [ ] Test: GET /profile/users/recommendations/mixed

---

## GĐ 4: OUTBOX PATTERN + EVENT-DRIVEN

> **Mục tiêu:** Đảm bảo reliable event publishing: khi save entity thành công,
> event CHẮC CHẮN được gửi tới Kafka (không mất event khi crash giữa chừng).
>
> **Ý tưởng:** Thay vì gửi Kafka trực tiếp, lưu event vào bảng outbox
> trong CÙNG transaction với entity → Scheduler poll outbox → gửi Kafka.

### 4.1 Identity Service (MySQL Outbox)

#### 4.1.1 OutboxEvent Entity

**File:** `IdentityService/src/.../entity/OutboxEvent.java`

```java
package org.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String aggregateType;       // "User"

    @Column(nullable = false)
    String aggregateId;         // userId

    @Column(nullable = false)
    String eventType;           // "USER_CREATED", "USER_DELETED"

    @Column(nullable = false)
    String topic;               // Kafka topic

    @Lob
    @Column(columnDefinition = "TEXT")
    String payload;             // JSON body

    @Column(columnDefinition = "TEXT")
    String headers;             // JSON headers (correlationId, sagaId)

    @Column(nullable = false)
    String status;              // PENDING, PUBLISHED, FAILED

    int retryCount;

    LocalDateTime createdAt;
    LocalDateTime publishedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        status = "PENDING";
        retryCount = 0;
    }

    public void markPublished() {
        status = "PUBLISHED";
        publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        status = "FAILED";
        retryCount++;
    }
}
```

#### 4.1.2 OutboxEventRepository

**File:** `IdentityService/src/.../repository/OutboxEventRepository.java`

```java
package org.identityservice.repository;

import org.identityservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' " +
           "AND o.retryCount < 5 ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
}
```

#### 4.1.3 OutboxDispatcherService

**File:** `IdentityService/src/.../service/OutboxDispatcherService.java`

```java
package org.identityservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.identityservice.entity.OutboxEvent;
import org.identityservice.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Dispatcher: poll bảng outbox_events mỗi 5 giây
 * và gửi các event PENDING lên Kafka.
 *
 * Flow:
 * 1. Query tất cả event status=PENDING, retryCount<5
 * 2. Với mỗi event → gửi Kafka (topic, key=aggregateId, value=payload)
 * 3. Nếu thành công → markPublished()
 * 4. Nếu lỗi → markFailed() (tăng retryCount)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxDispatcherService {

    OutboxEventRepository outboxEventRepository;
    KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.dispatcher.delay:5000}")
    @Transactional
    public void dispatchPendingEvents() {
        var events = outboxEventRepository.findPendingEvents();

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload()
                ).get(); // Đồng bộ để đảm bảo gửi thành công

                event.markPublished();
                outboxEventRepository.save(event);

                log.debug("Dispatched event: type={}, aggregateId={}",
                        event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                event.markFailed();
                outboxEventRepository.save(event);
                log.error("Failed to dispatch event: id={}, type={}, retry={}",
                        event.getId(), event.getEventType(), event.getRetryCount(), e);
            }
        }

        if (!events.isEmpty()) {
            log.info("Dispatched {}/{} outbox events",
                    events.stream().filter(e -> "PUBLISHED".equals(e.getStatus())).count(),
                    events.size());
        }
    }
}
```

### 4.2 Profile Service (Neo4j Outbox)

#### 4.2.1 OutboxEventNode

**File:** `profile-service/src/.../entity/OutboxEventNode.java`

```java
package com.blur.profileservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("OutboxEvent")
public class OutboxEventNode {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    @Property("aggregateType")
    String aggregateType;

    @Property("aggregateId")
    String aggregateId;

    @Property("eventType")
    String eventType;

    @Property("topic")
    String topic;

    @Property("payload")
    String payload;

    @Property("headers")
    String headers;

    @Property("status")
    String status;

    @Property("retryCount")
    int retryCount;

    @Property("createdAt")
    LocalDateTime createdAt;

    @Property("publishedAt")
    LocalDateTime publishedAt;

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.retryCount++;
    }
}
```

#### 4.2.2 OutboxEventNodeRepository

**File:** `profile-service/src/.../repository/OutboxEventNodeRepository.java`

```java
package com.blur.profileservice.repository;

import com.blur.profileservice.entity.OutboxEventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxEventNodeRepository extends Neo4jRepository<OutboxEventNode, String> {

    @Query("MATCH (e:OutboxEvent) WHERE e.status = 'PENDING' AND e.retryCount < 5 " +
           "RETURN e ORDER BY e.createdAt ASC LIMIT 100")
    List<OutboxEventNode> findPendingEvents();
}
```

#### 4.2.3 ProfileOutboxDispatcherService

**File:** `profile-service/src/.../service/ProfileOutboxDispatcherService.java`

```java
package com.blur.profileservice.service;

import com.blur.profileservice.entity.OutboxEventNode;
import com.blur.profileservice.repository.OutboxEventNodeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileOutboxDispatcherService {

    OutboxEventNodeRepository outboxRepository;
    KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.dispatcher.delay:5000}")
    public void dispatchPendingEvents() {
        var events = outboxRepository.findPendingEvents();

        for (OutboxEventNode event : events) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload()
                ).get();

                event.markPublished();
                outboxRepository.save(event);
            } catch (Exception e) {
                event.markFailed();
                outboxRepository.save(event);
                log.error("Failed to dispatch Neo4j outbox event: {}", event.getId(), e);
            }
        }
    }
}
```

### 4.3 Sửa UserService - Tích hợp Outbox

**Sửa:** `IdentityService/src/.../service/UserService.java` method `createUser`

```java
// THÊM imports
import org.identityservice.entity.OutboxEvent;
import org.identityservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

// THÊM inject
private final OutboxEventRepository outboxEventRepository;
private final ObjectMapper objectMapper;

// SỬA createUser()
@Transactional
public UserResponse createUser(UserCreationRequest request) {
    // ... existing user creation logic ...

    userRepository.save(user);

    // Tạo outbox event TRONG CÙNG TRANSACTION
    String payload = objectMapper.writeValueAsString(Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "firstName", request.getFirstName(),
            "lastName", request.getLastName()
    ));

    OutboxEvent outboxEvent = OutboxEvent.builder()
            .aggregateType("User")
            .aggregateId(user.getId())
            .eventType("USER_CREATED")
            .topic("user-events")
            .payload(payload)
            .build();

    outboxEventRepository.save(outboxEvent);

    // Profile creation
    var profileRequest = profileMapper.toProfileCreationRequest(request);
    profileRequest.setUserId(user.getId());
    userProfileService.createProfile(profileRequest);

    return userMapper.toUserResponse(user);
}
```

### 4.4 application.yaml - Thêm Kafka + Outbox Config

```yaml
# Thêm vào IdentityService application.yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9093}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: identity-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

outbox:
  dispatcher:
    delay: 5000  # Poll mỗi 5 giây
```

### 4.5 Checklist GĐ 4

- [ ] Tạo OutboxEvent entity (MySQL)
- [ ] Tạo OutboxEventRepository
- [ ] Tạo OutboxDispatcherService
- [ ] Sửa UserService.createUser() → thêm outbox event
- [ ] Sửa UserService.deleteUser() → thêm outbox event
- [ ] Thêm @EnableScheduling vào IdentityServiceApplication
- [ ] Tạo OutboxEventNode (Neo4j) trong profile-service
- [ ] Tạo OutboxEventNodeRepository
- [ ] Tạo ProfileOutboxDispatcherService
- [ ] Thêm Kafka config vào application.yaml
- [ ] Thêm spring-kafka dependency
- [ ] Test: User signup → outbox event saved → dispatched → Kafka message

---

## GĐ 5: SAGA CHOREOGRAPHY - DELETE USER

> **Mục tiêu:** Khi xóa user → xóa profile + posts + conversations + notifications.
> Dùng Saga Choreography (không cần orchestrator service riêng).

### 5.1 Luồng Saga Delete User

```
Identity Service                    Profile Service                Content Service
     │                                   │                              │
 1. DELETE /users/{id}                   │                              │
 2. Save SagaState(DELETE_USER)          │                              │
 3. OutboxEvent → Kafka                  │                              │
    "user-events" :                      │                              │
    {type: USER_DELETE_REQUESTED}        │                              │
     │                                   │                              │
     │  ──────────── Kafka ───────────>  │                              │
     │                               4. Delete UserProfile              │
     │                               5. OutboxEvent → Kafka             │
     │                                  {type: PROFILE_DELETED}         │
     │                                   │── Kafka ──────────────────>  │
     │                                   │                          6. Delete Posts
     │                                   │                          7. Delete Comments
     │                                   │                          8. EVENT: CONTENT_DELETED
     │  <──────── Kafka ──────────────   │                              │
 9. Nhận PROFILE_DELETED                 │                              │
10. Update SagaState → COMPLETED         │                              │
11. Hard delete User                     │                              │
```

### 5.2 SagaState Entity

**File:** `IdentityService/src/.../entity/SagaState.java`

```java
package org.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "saga_states")
public class SagaState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String sagaType;        // "CREATE_USER", "DELETE_USER"

    @Column(nullable = false)
    String aggregateId;     // userId

    @Column(nullable = false)
    String status;          // STARTED, PROFILE_COMPLETED, COMPLETED, COMPENSATING, FAILED

    @Lob
    @Column(columnDefinition = "TEXT")
    String payload;         // JSON snapshot of data

    int stepCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        stepCount = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        stepCount++;
    }
}
```

### 5.3 SagaStateRepository

**File:** `IdentityService/src/.../repository/SagaStateRepository.java`

```java
package org.identityservice.repository;

import org.identityservice.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {
    Optional<SagaState> findByAggregateIdAndSagaType(String aggregateId, String sagaType);
}
```

### 5.4 SagaConsumerService

**File:** `IdentityService/src/.../service/SagaConsumerService.java`

```java
package org.identityservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.identityservice.repository.SagaStateRepository;
import org.identityservice.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SagaConsumerService {

    SagaStateRepository sagaStateRepository;
    UserRepository userRepository;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "profile-saga-events", groupId = "identity-service")
    @Transactional
    public void consumeProfileSagaEvent(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(json, Map.class);
            String eventType = (String) event.get("eventType");
            String userId = (String) event.get("userId");

            switch (eventType) {
                case "PROFILE_CREATED" -> handleProfileCreated(userId);
                case "PROFILE_DELETED" -> handleProfileDeleted(userId);
                case "PROFILE_DELETE_FAILED" -> handleProfileDeleteFailed(userId);
                default -> log.warn("Unknown saga event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process saga event", e);
        }
    }

    private void handleProfileCreated(String userId) {
        sagaStateRepository.findByAggregateIdAndSagaType(userId, "CREATE_USER")
                .ifPresent(saga -> {
                    saga.setStatus("COMPLETED");
                    sagaStateRepository.save(saga);
                    log.info("Saga CREATE_USER completed for user: {}", userId);
                });
    }

    private void handleProfileDeleted(String userId) {
        sagaStateRepository.findByAggregateIdAndSagaType(userId, "DELETE_USER")
                .ifPresent(saga -> {
                    saga.setStatus("COMPLETED");
                    sagaStateRepository.save(saga);
                    // Hard delete user sau khi profile đã xóa
                    userRepository.deleteById(userId);
                    log.info("Saga DELETE_USER completed, user hard-deleted: {}", userId);
                });
    }

    private void handleProfileDeleteFailed(String userId) {
        sagaStateRepository.findByAggregateIdAndSagaType(userId, "DELETE_USER")
                .ifPresent(saga -> {
                    saga.setStatus("FAILED");
                    sagaStateRepository.save(saga);
                    log.error("Saga DELETE_USER FAILED for user: {} → compensation needed", userId);
                });
    }
}
```

### 5.5 ProfileSagaConsumerService

**File:** `profile-service/src/.../service/ProfileSagaConsumerService.java`

```java
package com.blur.profileservice.service;

import com.blur.profileservice.entity.OutboxEventNode;
import com.blur.profileservice.repository.OutboxEventNodeRepository;
import com.blur.profileservice.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileSagaConsumerService {

    UserProfileRepository userProfileRepository;
    OutboxEventNodeRepository outboxRepository;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "profile-service")
    public void consumeUserEvent(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(json, Map.class);
            String eventType = (String) event.get("eventType");
            String userId = (String) event.get("userId");

            switch (eventType) {
                case "USER_DELETE_REQUESTED" -> handleDeleteUser(userId);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process user event", e);
        }
    }

    private void handleDeleteUser(String userId) {
        try {
            userProfileRepository.findByUserId(userId).ifPresent(profile -> {
                userProfileRepository.delete(profile);
                log.info("Profile deleted for user: {}", userId);

                // Publish reply event qua outbox
                OutboxEventNode replyEvent = OutboxEventNode.builder()
                        .aggregateType("UserProfile")
                        .aggregateId(userId)
                        .eventType("PROFILE_DELETED")
                        .topic("profile-saga-events")
                        .payload("{\"eventType\":\"PROFILE_DELETED\",\"userId\":\"" + userId + "\"}")
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .retryCount(0)
                        .build();
                outboxRepository.save(replyEvent);
            });
        } catch (Exception e) {
            log.error("Failed to delete profile for user: {}", userId, e);
            // Publish failure event
            OutboxEventNode failEvent = OutboxEventNode.builder()
                    .aggregateType("UserProfile")
                    .aggregateId(userId)
                    .eventType("PROFILE_DELETE_FAILED")
                    .topic("profile-saga-events")
                    .payload("{\"eventType\":\"PROFILE_DELETE_FAILED\",\"userId\":\"" + userId + "\"}")
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();
            outboxRepository.save(failEvent);
        }
    }
}
```

### 5.6 Checklist GĐ 5

- [ ] Tạo SagaState entity + repository
- [ ] Sửa UserService.deleteUser() → tạo saga + outbox event
- [ ] Tạo SagaConsumerService (Identity Service)
- [ ] Tạo ProfileSagaConsumerService (Profile Service)
- [ ] Test: Delete user → saga started → profile deleted → saga completed → user hard-deleted
- [ ] Test compensation: simulate profile delete failure → saga FAILED

---

*Tiếp theo: GĐ 6-10 (CQRS, Redis, Keycloak, Resilience4j, Rate Limiting)*
*Xem file: `blur-part-b-gd6-onwards.md`*
