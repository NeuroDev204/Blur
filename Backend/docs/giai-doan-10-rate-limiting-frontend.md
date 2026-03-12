# Giai Đoạn 10: Rate Limiting + Frontend Upgrades (CHƯA TRIỂN KHAI)

## Mục tiêu

Bảo vệ API khỏi abuse + cập nhật frontend tương thích với các thay đổi backend.

## Trạng thái: CHƯA TRIỂN KHAI

## Phần A: Rate Limiting trong API Gateway

### API Gateway hiện tại

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/AuthenticationFilter.java`

Hiện tại API Gateway chỉ có:
- Routing theo path prefix (`/api/auth/**`, `/api/post/**`, etc.)
- Authentication filter (token introspection qua user-service)
- CORS configuration
- WebSocket routing (`/ws/**`)

**KHÔNG CÓ** rate limiting → bất kỳ ai cũng có thể spam unlimited requests.

### Bước 1: Thêm dependency

**File:** `api-gateway/pom.xml`

```xml
<!-- Spring Cloud Gateway đã có sẵn -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### Bước 2: Cấu hình Rate Limiter

**File:** `api-gateway/src/main/resources/application.yaml`

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 10       # 10 tokens/second
              burstCapacity: 20       # Burst tối đa 20 requests
              requestedTokens: 1      # Mỗi request tốn 1 token
            key-resolver: "#{@userKeyResolver}"
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 0
```

### Bước 3: Tạo UserKeyResolver

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/RateLimiterConfig.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Rate limit theo userId (từ JWT) nếu authenticated
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            // Fallback: rate limit theo IP
            return Mono.just(
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            );
        };
    }
}
```

### Bước 4: Thêm X-User-Id header sau authentication

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/AuthenticationFilter.java`

Sau khi introspect token thành công, thêm userId vào header:

```java
// Trong method filter(), sau khi token valid:
exchange.getRequest().mutate()
    .header("X-User-Id", userId)
    .build();
```

---

## Phần B: Frontend Upgrades

### Hiện trạng Frontend

Frontend hiện tại đã sử dụng:
- **STOMP over SockJS** cho real-time messaging (`SocketContext.tsx`)
- **STOMP notifications** (`NotificationSocketContext.tsx`)
- **WebRTC** cho video/audio calls (`useCall.ts`, `WebRTCService.ts`)
- **Axios with HttpOnly cookies** (`axiosClient.ts`)
- **Moderation listener** (`useModerationListener.ts`)

### Bước 5: Xử lý HTTP 429 (Rate Limited) trong Frontend

**File:** `frontend/src/api/axiosClient.ts`

Thêm interceptor xử lý rate limit response:

```typescript
import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8888/api',
    withCredentials: true,
});

// Response interceptor cho rate limiting
axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 429) {
            // Rate limited - hiển thị thông báo cho user
            const retryAfter = error.response.headers['retry-after'];
            console.warn(`Rate limited. Retry after ${retryAfter || '1'}s`);

            // Có thể hiển thị toast notification
            // toast.error('Bạn đang gửi quá nhiều request. Vui lòng đợi một chút.');

            // Auto-retry sau delay
            return new Promise((resolve) => {
                setTimeout(() => {
                    resolve(axiosClient(error.config));
                }, (parseInt(retryAfter) || 1) * 1000);
            });
        }
        return Promise.reject(error);
    }
);

export default axiosClient;
```

### Bước 6: Thêm request throttling cho actions thường xuyên

**File:** `frontend/src/utils/throttle.ts`

```typescript
/**
 * Throttle function - giới hạn tần suất gọi function.
 * Dùng cho like, comment, follow actions.
 */
export function throttle<T extends (...args: any[]) => any>(
    func: T,
    limit: number
): (...args: Parameters<T>) => ReturnType<T> | undefined {
    let inThrottle = false;
    let lastResult: ReturnType<T>;

    return function (this: any, ...args: Parameters<T>) {
        if (!inThrottle) {
            lastResult = func.apply(this, args);
            inThrottle = true;
            setTimeout(() => (inThrottle = false), limit);
            return lastResult;
        }
        return undefined;
    };
}
```

### Bước 7: Áp dụng throttle cho PostCard actions

**File:** `frontend/src/Components/Post/PostCard.tsx`

```typescript
import { throttle } from '../../utils/throttle';

// Trong component:
const throttledLike = useMemo(
    () => throttle(async () => {
        await postApi.likePost(post.id);
    }, 1000), // Tối đa 1 like/second
    [post.id]
);

const throttledSave = useMemo(
    () => throttle(async () => {
        await postApi.savePost(post.id);
    }, 1000),
    [post.id]
);
```

### Bước 8: Hiển thị Moderation Status trên Comment

Frontend đã có `ModerationWarningModal.tsx` và `useModerationListener.ts`. Cần thêm badge hiển thị trạng thái moderation trên mỗi comment.

**File:** `frontend/src/Components/Comment/CommentCard.tsx`

Thêm moderation badge:

```tsx
const ModerationBadge: React.FC<{ status: string | null }> = ({ status }) => {
    if (!status || status === 'APPROVED') return null;

    const styles: Record<string, { bg: string; text: string; label: string }> = {
        PENDING: { bg: '#fef3cd', text: '#856404', label: 'Đang kiểm duyệt' },
        REJECTED: { bg: '#f8d7da', text: '#721c24', label: 'Vi phạm' },
    };

    const style = styles[status];
    if (!style) return null;

    return (
        <span className="px-2 py-0.5 rounded text-xs font-medium"
              style={{ backgroundColor: style.bg, color: style.text }}>
            {style.label}
        </span>
    );
};
```

---

## Hướng dẫn Test

### Test 1: Rate limiter cho phép trong ngưỡng

```bash
# Gửi 10 requests (trong ngưỡng replenishRate = 10/s)
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8888/api/post/all?page=0&limit=10 \
    -H "Authorization: Bearer <TOKEN>")
  echo "Request $i: HTTP $STATUS"
done
# Mong đợi: tất cả HTTP 200
```

### Test 2: Rate limiter chặn khi vượt ngưỡng

```bash
# Gửi 30 requests liên tiếp (vượt burstCapacity = 20)
for i in {1..30}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8888/api/post/all?page=0&limit=10 \
    -H "Authorization: Bearer <TOKEN>")
  echo "Request $i: HTTP $STATUS"
done
# Mong đợi: ~20 requests đầu HTTP 200, sau đó HTTP 429
```

### Test 3: Redis rate limit keys

```bash
docker exec -it redis redis-cli

KEYS request_rate_limiter*
GET "request_rate_limiter.{user-id-xxx}.tokens"
GET "request_rate_limiter.{user-id-xxx}.timestamp"
```

### Test 4: Rate limit reset

```bash
# Spam vượt ngưỡng
for i in {1..25}; do
  curl -s -o /dev/null http://localhost:8888/api/post/all?page=0&limit=10 \
    -H "Authorization: Bearer <TOKEN>"
done

# Đợi 1s → tokens được bổ sung
sleep 1

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8888/api/post/all?page=0&limit=10 \
  -H "Authorization: Bearer <TOKEN>")
echo "After reset: HTTP $STATUS"
# Mong đợi: HTTP 200
```

### Test 5: Frontend rate limit handling

1. Mở browser → F12 → Network tab
2. Click Like nhanh nhiều lần → throttle giới hạn 1 request/second
3. Nếu server trả 429 → auto-retry sau delay

## Checklist

- [ ] Thêm spring-boot-starter-data-redis-reactive vào api-gateway
- [ ] Cấu hình RequestRateLimiter trong application.yaml (10 req/s, burst 20)
- [ ] Tạo RateLimiterConfig với UserKeyResolver (userId hoặc IP)
- [ ] Sửa AuthenticationFilter thêm X-User-Id header sau introspection
- [ ] Frontend: thêm 429 interceptor trong axiosClient.ts
- [ ] Frontend: tạo throttle utility
- [ ] Frontend: áp dụng throttle cho like, save, follow actions
- [ ] Frontend: thêm ModerationBadge cho CommentCard
- [ ] Test: 10 req/s → HTTP 200
- [ ] Test: 30 req/s → HTTP 429 sau ~20 requests
- [ ] Test: Redis rate limit keys tồn tại
- [ ] Test: rate limit reset sau 1s
- [ ] Test: frontend throttle hoạt động
