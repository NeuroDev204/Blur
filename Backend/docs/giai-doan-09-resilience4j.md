# Giai Đoạn 9: Resilience4j (CHƯA TRIỂN KHAI)

## Mục tiêu

Circuit breaker + retry + timeout cho inter-service calls.
Bảo vệ hệ thống khi một service bị lỗi hoặc chậm, tránh cascading failure.

## Trạng thái: CHƯA TRIỂN KHAI

## Vấn đề hiện tại

Các services gọi nhau qua Feign client KHÔNG có protection:

```
Content Service                     User Service
───────────────                    ─────────────
PostService.createPost()
  │
  └─ profileClient.getProfile()  ────→  User Service (8081)
                                           │
                                    Nếu user-service DOWN:
                                    - Feign timeout (mặc định 60s)
                                    - Thread bị block 60s
                                    - Nhiều requests → thread pool exhausted
                                    - Content service cũng DOWN (cascading failure!)
```

**Inter-service calls hiện tại (không có protection):**

| Caller | Callee | Feign Client | Method |
|--------|--------|-------------|--------|
| content-service | user-service | `ProfileClient` | `getProfile(userId)`, `getProfileByProfileId(profileId)` |
| content-service | communication-service | `CommunicationServiceClient` | `sendModerationUpdate(result)` |
| api-gateway | user-service | `ProfileAuthClient` | `introspect(token)` |

## Bước 1: Thêm dependency vào pom.xml

**File:** `pom.xml` cho content-service và communication-service

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Bước 2: Cấu hình application.yaml

**File:** `content-service/src/main/resources/application.yaml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      profileService:
        sliding-window-size: 10           # Đếm 10 calls gần nhất
        failure-rate-threshold: 50        # Mở circuit khi 50% fail
        wait-duration-in-open-state: 10s  # Đợi 10s trước khi thử lại
        permitted-number-of-calls-in-half-open-state: 3  # Cho 3 calls thử
      communicationService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s

  retry:
    instances:
      profileService:
        max-attempts: 3                   # Retry tối đa 3 lần
        wait-duration: 500ms              # Đợi 500ms giữa mỗi lần
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - feign.FeignException.ServiceUnavailable
      communicationService:
        max-attempts: 2
        wait-duration: 1s
        retry-exceptions:
          - java.io.IOException

  timelimiter:
    instances:
      profileService:
        timeout-duration: 3s              # Timeout sau 3s
      communicationService:
        timeout-duration: 5s

# Actuator endpoints để monitor
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,circuitbreakerevents
  health:
    circuitbreakers:
      enabled: true
  endpoint:
    health:
      show-details: always
```

## Bước 3: Tạo Resilient Service Wrapper cho Content Service

**File:** `content-service/src/main/java/com/contentservice/post/service/ResilientProfileService.java`

```java
package com.contentservice.post.service;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.story.dto.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResilientProfileService {
    ProfileClient profileClient;

    @CircuitBreaker(name = "profileService", fallbackMethod = "getProfileFallback")
    @Retry(name = "profileService")
    public ApiResponse<UserProfileResponse> getProfile(String userId) {
        return profileClient.getProfile(userId);
    }

    /**
     * Fallback khi user-service down hoặc circuit open.
     * Trả về profile tối thiểu để post/comment vẫn hoạt động.
     */
    public ApiResponse<UserProfileResponse> getProfileFallback(String userId, Exception e) {
        log.warn("Fallback triggered for getProfile({}): {}", userId, e.getMessage());
        UserProfileResponse fallback = UserProfileResponse.builder()
                .userId(userId)
                .username("user_" + userId.substring(0, Math.min(6, userId.length())))
                .firstName("Unknown")
                .lastName("User")
                .build();
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(fallback)
                .build();
    }

    @CircuitBreaker(name = "profileService", fallbackMethod = "getProfileByIdFallback")
    @Retry(name = "profileService")
    public ApiResponse<UserProfileResponse> getProfileByProfileId(String profileId) {
        return profileClient.getProfileByProfileId(profileId);
    }

    public ApiResponse<UserProfileResponse> getProfileByIdFallback(String profileId, Exception e) {
        log.warn("Fallback triggered for getProfileByProfileId({}): {}", profileId, e.getMessage());
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(UserProfileResponse.builder()
                        .id(profileId)
                        .firstName("Unknown")
                        .lastName("User")
                        .build())
                .build();
    }
}
```

## Bước 4: Tạo Resilient Moderation Notification

**File:** `content-service/src/main/java/com/contentservice/post/service/ResilientCommunicationService.java`

```java
package com.contentservice.post.service;

import com.contentservice.client.CommunicationServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResilientCommunicationService {
    CommunicationServiceClient communicationServiceClient;

    @CircuitBreaker(name = "communicationService", fallbackMethod = "sendModerationUpdateFallback")
    @Retry(name = "communicationService")
    public void sendModerationUpdate(Map<String, Object> result) {
        communicationServiceClient.sendModerationUpdate(result);
    }

    public void sendModerationUpdateFallback(Map<String, Object> result, Exception e) {
        log.warn("Fallback: moderation update not sent to communication-service for comment {}: {}",
                result.get("commentId"), e.getMessage());
        // Moderation result đã lưu trong DB, user sẽ thấy khi refresh
    }
}
```

## Bước 5: Sửa PostService và ModerationResultConsumer dùng Resilient services

### PostService

Thay `profileClient.getProfile()` bằng `resilientProfileService.getProfile()`:

```java
// Trước:
var profileResult = profileClient.getProfile(userId).getResult();

// Sau:
var profileResult = resilientProfileService.getProfile(userId).getResult();
```

### ModerationResultConsumer

Thay `communicationServiceClient.sendModerationUpdate()` bằng `resilientCommunicationService.sendModerationUpdate()`:

```java
// Trước:
communicationServiceClient.sendModerationUpdate(result);

// Sau:
resilientCommunicationService.sendModerationUpdate(result);
```

## Hướng dẫn Test

### Test 1: Circuit breaker CLOSED (bình thường)

```bash
# Gọi API bình thường
curl -X GET http://localhost:8888/api/post/all?page=0&limit=10 \
  -H "Authorization: Bearer <TOKEN>"

# Kiểm tra circuit breaker qua Actuator
curl -s http://localhost:8082/actuator/health | jq '.components.circuitBreakers'
# Mong đợi: state = "CLOSED", failedCalls = 0
```

### Test 2: Circuit breaker OPEN (service down)

```bash
# Tắt user-service
docker-compose stop user-service

# Gọi API 10+ lần
for i in {1..12}; do
  curl -s http://localhost:8888/api/post/all?page=0&limit=10 \
    -H "Authorization: Bearer <TOKEN>" | jq '.code'
done

# Kiểm tra: circuit breaker chuyển sang OPEN
curl -s http://localhost:8082/actuator/health | jq '.components.circuitBreakers'
# Mong đợi: state = "OPEN", failureRate = "100.0%"

# API vẫn trả về data (fallback) nhưng author info là "Unknown User"
```

### Test 3: Circuit breaker recovery (HALF_OPEN → CLOSED)

```bash
# Bật lại user-service
docker-compose start user-service

# Đợi 10s (wait-duration-in-open-state)
sleep 10

# Gọi API 3 lần (permitted-number-of-calls-in-half-open-state)
for i in {1..3}; do
  curl -s http://localhost:8888/api/post/all?page=0&limit=10 \
    -H "Authorization: Bearer <TOKEN>" | jq '.code'
done

# Kiểm tra: circuit breaker về CLOSED
curl -s http://localhost:8082/actuator/health | jq '.components.circuitBreakers'
# Mong đợi: state = "CLOSED"
```

### Test 4: Xem circuit breaker events

```bash
curl -s http://localhost:8082/actuator/circuitbreakerevents | jq '.circuitBreakerEvents'
# Mong đợi: SUCCESS, ERROR, STATE_TRANSITION events
```

## Checklist

- [ ] Thêm resilience4j-spring-boot3 dependency vào content-service
- [ ] Thêm spring-boot-starter-aop dependency
- [ ] Thêm spring-boot-starter-actuator dependency
- [ ] Config circuit breaker cho profileService trong application.yaml
- [ ] Config circuit breaker cho communicationService trong application.yaml
- [ ] Config retry cho transient failures
- [ ] Config timelimiter timeout
- [ ] Tạo ResilientProfileService với fallback methods
- [ ] Tạo ResilientCommunicationService với fallback methods
- [ ] Sửa PostService → dùng ResilientProfileService
- [ ] Sửa ModerationResultConsumer → dùng ResilientCommunicationService
- [ ] Bật Actuator endpoints: circuitbreakers, circuitbreakerevents
- [ ] Test: CLOSED state khi services healthy
- [ ] Test: OPEN state + fallback khi service down
- [ ] Test: HALF_OPEN → CLOSED recovery
