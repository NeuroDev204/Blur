# Giai Đoạn 9: Resilience4j

## Mục tiêu

Circuit breaker + retry + rate limiting cho inter-service calls.
Bảo vệ hệ thống khi một service bị lỗi hoặc chậm, tránh cascading failure.

## Bước 1: Thêm dependency vào pom.xml

Thêm vào `pom.xml` của mỗi service cần gọi inter-service (content-service, profile-service, communication-service):

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

Thêm vào `application.yaml` của mỗi service:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      profileService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
      identityService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s

  retry:
    instances:
      profileService:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
      identityService:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException

  timelimiter:
    instances:
      profileService:
        timeout-duration: 3s
      identityService:
        timeout-duration: 3s

# Actuator endpoints để monitor circuit breaker
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

## Bước 3: Sử dụng trong Content Service

**File:** `content-service/src/main/java/com/contentservice/post/service/ContentService.java`

```java
package com.contentservice.post.service;

import com.contentservice.post.dto.response.UserProfileResponse;
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
public class ContentService {

    ProfileClient profileClient;

    @CircuitBreaker(name = "profileService", fallbackMethod = "getProfileFallback")
    @Retry(name = "profileService")
    public UserProfileResponse getProfile(String userId) {
        return profileClient.getProfile(userId).getResult();
    }

    public UserProfileResponse getProfileFallback(String userId, Exception e) {
        log.warn("Fallback triggered for getProfile({}): {}", userId, e.getMessage());
        return UserProfileResponse.builder()
                .userId(userId)
                .username("user_" + userId.substring(0, 6))
                .firstName("Unknown")
                .lastName("User")
                .build();
    }
}
```

## Bước 4: Sử dụng trong Communication Service

**File:** `communication-service/src/main/java/com/blur/communicationservice/service/CommunicationService.java`

```java
package com.blur.communicationservice.service;

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
public class CommunicationService {

    IdentityClient identityClient;

    @CircuitBreaker(name = "identityService", fallbackMethod = "validateTokenFallback")
    @Retry(name = "identityService")
    public boolean validateToken(String token) {
        return identityClient.validateToken(token).getResult();
    }

    public boolean validateTokenFallback(String token, Exception e) {
        log.warn("Fallback triggered for validateToken: {}", e.getMessage());
        return false;
    }
}
```

## Bước 5: Tạo DTO cho fallback response

**File:** `content-service/src/main/java/com/contentservice/post/dto/response/UserProfileResponse.java`

```java
package com.contentservice.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    String userId;
    String username;
    String firstName;
    String lastName;
    String avatar;
    String bio;
}
```

## Hướng dẫn Test

### Test 1: Circuit breaker hoạt động bình thường (CLOSED state)

**Bước 1:** Đảm bảo tất cả services đang chạy.

```bash
docker-compose up -d
```

**Bước 2:** Gọi API bình thường → circuit breaker ở trạng thái CLOSED.

```bash
curl -X GET http://localhost:8888/api/content/posts \
  -H "Authorization: Bearer <TOKEN>"
```

**Bước 3:** Kiểm tra trạng thái circuit breaker qua Actuator.

```bash
curl -s http://localhost:<CONTENT_SERVICE_PORT>/actuator/health | jq '.components.circuitBreakers'
```

Response mong đợi:
```json
{
  "status": "UP",
  "details": {
    "profileService": {
      "status": "UP",
      "details": {
        "failureRate": "-1.0%",
        "state": "CLOSED",
        "bufferedCalls": 1,
        "failedCalls": 0
      }
    }
  }
}
```

### Test 2: Circuit breaker mở khi service bị lỗi (OPEN state)

**Bước 1:** Tắt profile-service.

```bash
docker-compose stop profile-service
```

**Bước 2:** Gọi API liên tục 10 lần (vượt sliding-window-size).

```bash
for i in {1..10}; do
  echo "Request $i:"
  curl -s -X GET http://localhost:8888/api/content/posts \
    -H "Authorization: Bearer <TOKEN>" | jq '.code'
  echo ""
done
```

**Bước 3:** Kiểm tra circuit breaker đã chuyển sang OPEN.

```bash
curl -s http://localhost:<CONTENT_SERVICE_PORT>/actuator/health | jq '.components.circuitBreakers'
```

Response mong đợi:
```json
{
  "details": {
    "profileService": {
      "details": {
        "failureRate": "100.0%",
        "state": "OPEN",
        "bufferedCalls": 10,
        "failedCalls": 10
      }
    }
  }
}
```

**Bước 4:** Gọi API tiếp → fallback trả về ngay lập tức (không gọi profile-service).

```bash
curl -s -X GET http://localhost:8888/api/content/posts \
  -H "Authorization: Bearer <TOKEN>"
```

Response vẫn trả về nhưng author info là "Unknown User" (fallback data).

### Test 3: Circuit breaker tự phục hồi (HALF_OPEN → CLOSED)

**Bước 1:** Khởi động lại profile-service.

```bash
docker-compose start profile-service
```

**Bước 2:** Đợi 10 giây (wait-duration-in-open-state).

**Bước 3:** Gọi API → circuit breaker chuyển sang HALF_OPEN, cho phép 3 calls thử.

```bash
for i in {1..3}; do
  curl -s -X GET http://localhost:8888/api/content/posts \
    -H "Authorization: Bearer <TOKEN>" | jq '.code'
done
```

**Bước 4:** Kiểm tra circuit breaker đã chuyển về CLOSED.

```bash
curl -s http://localhost:<CONTENT_SERVICE_PORT>/actuator/health | jq '.components.circuitBreakers'
```

Response mong đợi: state = "CLOSED".

### Test 4: Retry hoạt động

**Bước 1:** Xem log content-service khi profile-service bị chậm.

```bash
docker logs -f content-service 2>&1 | grep -i "retry"
```

Mong đợi: thấy 3 lần retry (max-attempts: 3) với khoảng cách 500ms giữa mỗi lần.

### Test 5: Xem circuit breaker events

```bash
curl -s http://localhost:<CONTENT_SERVICE_PORT>/actuator/circuitbreakerevents | jq '.circuitBreakerEvents'
```

Response mong đợi: danh sách events bao gồm SUCCESS, ERROR, STATE_TRANSITION.

## Checklist

- [ ] Thêm resilience4j-spring-boot3 dependency vào content-service
- [ ] Thêm resilience4j-spring-boot3 dependency vào communication-service
- [ ] Thêm spring-boot-starter-aop dependency (bắt buộc cho annotation)
- [ ] Config circuit breaker cho profileService trong application.yaml
- [ ] Config circuit breaker cho identityService trong application.yaml
- [ ] Config retry cho transient failures (IOException, TimeoutException)
- [ ] Config timelimiter timeout 3 giây
- [ ] Implement fallback method cho getProfile()
- [ ] Implement fallback method cho validateToken()
- [ ] Bật Actuator endpoints: circuitbreakers, circuitbreakerevents
- [ ] Test: gọi API bình thường → circuit breaker CLOSED
- [ ] Test: tắt profile-service → circuit breaker OPEN → fallback trả về
- [ ] Test: bật lại profile-service → circuit breaker HALF_OPEN → CLOSED
- [ ] Test: retry 3 lần khi gặp transient error
- [ ] Test: kiểm tra Actuator endpoint hiển thị đúng trạng thái
