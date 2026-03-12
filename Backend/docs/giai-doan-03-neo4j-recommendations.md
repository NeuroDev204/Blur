# Giai Đoạn 3: Neo4j Follow Recommendation (ĐÃ HOÀN THÀNH)

## Mục tiêu

Sử dụng Neo4j graph database để gợi ý bạn bè dựa trên:
1. **Mutual connections** - bạn của bạn
2. **Similar taste** - follow cùng người
3. **Same city** - cùng thành phố
4. **Popular users** - người dùng phổ biến (nhiều followers)
5. **Combined** - kết hợp tất cả với trọng số

## Trạng thái: ĐÃ HOÀN THÀNH

## Kiến trúc

```
Frontend                         User Service (8081)                    Neo4j
────────                        ────────────────────                  ─────
GET /recommendations/mutual  →   RecommendationController
                                        │
                                  UserProfileService
                                  .getMutualRecommendations()
                                        │
                                  @Cacheable("recommendations")
                                        │
                                  UserProfileRepository
                                  .findMutualRecommendations()
                                        │
                                  Cypher Query ──────────────────→  Graph Traversal
                                                                    (me)-[:follows]->(friend)
                                                                    (friend)-[:follows]->(recommended)
                                                                    WHERE NOT (me)-[:follows]->(recommended)
```

## API Endpoints

### RecommendationController

**File:** `user-service/src/main/java/com/blur/userservice/profile/controller/RecommendationController.java`

| Endpoint | Mô tả | Params |
|----------|--------|--------|
| `GET /recommendations/mutual` | Gợi ý dựa trên kết nối chung | `page`, `size` |
| `GET /recommendations/nearby` | Gợi ý cùng thành phố | `page`, `size` |
| `GET /recommendations/similar` | Gợi ý sở thích tương tự | `page`, `size` |
| `GET /recommendations/popular` | Gợi ý người dùng phổ biến | `minFollowers`, `page`, `size` |

```java
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {
    UserProfileService userProfileService;

    @GetMapping("/mutual")
    public ApiResponse<RecommendationPageResponse> getMutualRecommendatApiResponse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getMutualRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder().result(result).build();
    }

    @GetMapping("/nearby")
    public ApiResponse<RecommendationPageResponse> getSameCityRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getSameCityRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder().result(result).build();
    }

    @GetMapping("/similar")
    public ApiResponse<RecommendationPageResponse> getMimilarRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getSimilarTasteRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder().result(result).build();
    }

    @GetMapping("/popular")
    public ApiResponse<RecommendationPageResponse> getPopularRecommendations(
            @RequestParam(defaultValue = "100") int minFollowers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = userProfileService.getPopularRecommendations(minFollowers, page, size);
        return ApiResponse.<RecommendationPageResponse>builder().result(result).build();
    }
}
```

## Neo4j Cypher Queries

### UserProfileRepository

**File:** `user-service/src/main/java/com/blur/userservice/profile/repository/UserProfileRepository.java`

### 1. Mutual Recommendations (bạn của bạn)

```cypher
MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
MATCH (myFollowing)-[:follows]->(recommended:user_profile)
WHERE recommended <> me
  AND NOT (me)-[:follows]->(recommended)
  AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
WITH recommended, COUNT(DISTINCT myFollowing) AS mutualCount
ORDER BY mutualCount DESC
SKIP $skip LIMIT $limit
RETURN recommended
```

**Logic:** Tìm user mà bạn bè của mình đang follow, sắp xếp theo số mutual connections giảm dần.

### 2. Similar Taste Recommendations (follow cùng người)

```cypher
MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
MATCH (recommended:user_profile)-[:follows]->(following)
WHERE recommended <> me
  AND NOT (me)-[:follows]->(recommended)
  AND NOT (recommended)-[:follows]->(me)
  AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
WITH recommended, COUNT(DISTINCT following) AS sharedCount
ORDER BY sharedCount DESC
SKIP $skip LIMIT $limit
RETURN recommended
```

**Logic:** Tìm user cũng follow cùng những người mà mình follow, sắp xếp theo số shared follows.

### 3. Same City Recommendations

```cypher
MATCH (me:user_profile {id: $userId})
WHERE me.city IS NOT NULL AND me.city <> ''
MATCH (recommended:user_profile)
WHERE recommended.city = me.city
  AND recommended <> me
  AND NOT (me)-[:follows]->(recommended)
ORDER BY recommended.followersCount DESC
SKIP $skip LIMIT $limit
RETURN recommended
```

**Logic:** Tìm user cùng thành phố, sắp xếp theo số followers.

### 4. Popular Recommendations

```cypher
MATCH (me:user_profile {id: $userId})
MATCH (recommended:user_profile)
WHERE recommended <> me
  AND NOT (me)-[:follows]->(recommended)
  AND recommended.followersCount >= $minFollowers
ORDER BY recommended.followersCount DESC
SKIP $skip LIMIT $limit
RETURN recommended
```

**Logic:** Tìm user phổ biến có `followersCount >= minFollowers`.

### 5. Combined Recommendations (trọng số kết hợp)

```cypher
MATCH (me:user_profile {id: $userId})
OPTIONAL MATCH (me)-[:follows]->(myFollowing)-[:follows]->(mutual:user_profile)
WHERE mutual <> me AND NOT (me)-[:follows]->(mutual)
WITH me, COLLECT(DISTINCT mutual) AS mutualRecs

OPTIONAL MATCH (me)-[:follows]->(following:user_profile)<-[:follows]-(similar:user_profile)
WHERE similar <> me AND NOT (me)-[:follows]->(similar)
WITH me, mutualRecs, COLLECT(DISTINCT similar) AS similarRecs

// ... kết hợp với same city và popular, tính weighted score
// mutual: 40%, city: 25%, similar: 25%, popular: 10%
```

## Response DTOs

### RecommendationResponse

```java
@Data @Builder
public class RecommendationResponse {
    String id;
    String userId;
    String username;
    String firstName;
    String lastName;
    String imageUrl;
    String bio;
    String city;
    long followerCount;
    long followingCount;
    int mutualConnections;
    RecommendationType recommendationType;  // MUTUAL, SAME_CITY, SIMILAR_TASTE, POPULAR, COMBINED
}
```

### RecommendationPageResponse

```java
@Data @Builder
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

## Caching

Tất cả recommendation queries đều được cache bằng Redis (DB 2):

```java
@Cacheable(value = "recommendations", key = "'mutual:' + #root.target.getCurrentProfileId() + ':' + #page + ':' + #size")
public RecommendationPageResponse getMutualRecommendations(int page, int size) { ... }
```

## Test Data

### TestDataController

**File:** `user-service/src/main/java/com/blur/userservice/profile/controller/TestDataController.java`

```
POST /test-data/generate?userCount=10000    → Tạo random test users
GET  /test-data/stats                       → Xem user count statistics
```

### InternalUserProfileController

```
POST /internal/generate-follows?min=5&max=20  → Generate random follows cho tất cả users
POST /internal/generate-cities                → Assign random cities
```

## Checklist

- [x] Tạo RecommendationController với 4 endpoints
- [x] Tạo Cypher queries cho mutual, similar taste, same city, popular, combined
- [x] Tạo RecommendationResponse và RecommendationPageResponse DTOs
- [x] Tạo RecommendationType enum
- [x] Cache recommendations bằng Redis
- [x] Tạo TestDataController để generate test data
- [x] Tạo internal endpoints để generate follows và cities
- [x] Pagination hỗ trợ (page, size, totalElements, totalPages)
