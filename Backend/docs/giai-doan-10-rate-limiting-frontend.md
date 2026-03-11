# Giai Đoạn 10: Rate Limiting + Frontend Upgrades

## Mục tiêu

Bảo vệ API khỏi abuse + cập nhật frontend tương thích.

## Phần A: Rate Limiting trong API Gateway

### Bước 1: Thêm dependency vào pom.xml

**File:** `api-gateway/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### Bước 2: Cấu hình Rate Limiter trong application.yaml

**File:** `api-gateway/src/main/resources/application.yaml`

Thêm cấu hình rate limiter:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 10       # 10 requests/second
              burstCapacity: 20      # burst tối đa 20
              requestedTokens: 1
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
        // Rate limit theo user ID (từ JWT header) hoặc IP
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            return Mono.just(
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            );
        };
    }
}
```

## Phần B: Frontend Upgrades

### Bước 4: Migrate WebSocket từ Socket.IO sang STOMP over SockJS

**File:** `frontend/src/api/websocket.ts`

```typescript
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

const GATEWAY_WS = 'http://localhost:8888/ws';

export const createStompClient = (token: string): Client => {
    return new Client({
        webSocketFactory: () => new SockJS(GATEWAY_WS),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => console.log('STOMP connected'),
        onDisconnect: () => console.log('STOMP disconnected'),
        reconnectDelay: 5000,
    });
};

export const subscribeToNotifications = (
    client: Client,
    userId: string,
    onMessage: (message: any) => void
): void => {
    client.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
        const notification = JSON.parse(message.body);
        onMessage(notification);
    });
};

export const subscribeToChat = (
    client: Client,
    conversationId: string,
    onMessage: (message: any) => void
): void => {
    client.subscribe(`/topic/chat/${conversationId}`, (message: IMessage) => {
        const chatMessage = JSON.parse(message.body);
        onMessage(chatMessage);
    });
};

export const sendChatMessage = (
    client: Client,
    conversationId: string,
    content: string
): void => {
    client.publish({
        destination: `/app/chat/${conversationId}`,
        body: JSON.stringify({ content }),
    });
};
```

### Bước 5: Cập nhật Notification subscription

**File:** `frontend/src/hooks/useNotifications.ts`

```typescript
import { useEffect, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { createStompClient, subscribeToNotifications } from '../api/websocket';

interface Notification {
    id: string;
    type: string;
    message: string;
    read: boolean;
    createdAt: string;
}

export const useNotifications = (token: string, userId: string) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [client, setClient] = useState<Client | null>(null);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        if (!token || !userId) return;

        const stompClient = createStompClient(token);

        stompClient.onConnect = () => {
            setConnected(true);
            subscribeToNotifications(stompClient, userId, (notification: Notification) => {
                setNotifications(prev => [notification, ...prev]);
            });
        };

        stompClient.onDisconnect = () => {
            setConnected(false);
        };

        stompClient.activate();
        setClient(stompClient);

        return () => {
            if (stompClient.active) {
                stompClient.deactivate();
            }
        };
    }, [token, userId]);

    const markAsRead = useCallback((notificationId: string) => {
        setNotifications(prev =>
            prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
        );
    }, []);

    return { notifications, connected, markAsRead };
};
```

### Bước 6: Hiển thị Moderation Status

**File:** `frontend/src/components/PostCard.tsx`

```tsx
import React from 'react';

interface Post {
    id: string;
    content: string;
    authorUsername: string;
    authorAvatar: string;
    moderationStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
    createdDate: string;
}

interface PostCardProps {
    post: Post;
}

const ModerationBadge: React.FC<{ status: Post['moderationStatus'] }> = ({ status }) => {
    const badgeStyles: Record<string, { backgroundColor: string; color: string; label: string }> = {
        PENDING: { backgroundColor: '#fef3cd', color: '#856404', label: 'Pending Review' },
        APPROVED: { backgroundColor: '#d4edda', color: '#155724', label: 'Approved' },
        REJECTED: { backgroundColor: '#f8d7da', color: '#721c24', label: 'Rejected' },
    };

    const style = badgeStyles[status];

    return (
        <span style={{
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: 'bold',
            backgroundColor: style.backgroundColor,
            color: style.color,
        }}>
            {style.label}
        </span>
    );
};

const PostCard: React.FC<PostCardProps> = ({ post }) => {
    return (
        <div style={{
            border: '1px solid #e0e0e0',
            borderRadius: '8px',
            padding: '16px',
            marginBottom: '12px',
            opacity: post.moderationStatus === 'REJECTED' ? 0.5 : 1,
        }}>
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '12px' }}>
                <img
                    src={post.authorAvatar}
                    alt={post.authorUsername}
                    style={{ width: '40px', height: '40px', borderRadius: '50%', marginRight: '8px' }}
                />
                <div>
                    <strong>{post.authorUsername}</strong>
                    <div style={{ fontSize: '12px', color: '#666' }}>{post.createdDate}</div>
                </div>
                <div style={{ marginLeft: 'auto' }}>
                    <ModerationBadge status={post.moderationStatus} />
                </div>
            </div>
            <p>{post.content}</p>
            {post.moderationStatus === 'REJECTED' && (
                <p style={{ color: '#721c24', fontSize: '14px', fontStyle: 'italic' }}>
                    This post has been rejected by moderation.
                </p>
            )}
        </div>
    );
};

export default PostCard;
```

### Bước 7: Cài đặt package cần thiết cho frontend

```bash
cd frontend
npm install sockjs-client @stomp/stompjs
npm install --save-dev @types/sockjs-client
```

## Hướng dẫn Test

### Test 1: Rate Limiter hoạt động (cho phép trong ngưỡng)

**Bước 1:** Đảm bảo API Gateway và Redis đang chạy.

```bash
docker-compose up -d redis api-gateway
```

**Bước 2:** Gửi 10 requests liên tiếp (trong ngưỡng replenishRate = 10/s).

```bash
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8888/api/content/posts \
    -H "Authorization: Bearer <TOKEN>")
  echo "Request $i: HTTP $STATUS"
done
```

Mong đợi: tất cả 10 requests trả về HTTP 200.

### Test 2: Rate Limiter chặn khi vượt ngưỡng

**Bước 3:** Gửi 30 requests liên tiếp (vượt burstCapacity = 20).

```bash
for i in {1..30}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8888/api/content/posts \
    -H "Authorization: Bearer <TOKEN>")
  echo "Request $i: HTTP $STATUS"
done
```

Mong đợi: khoảng 20 requests đầu trả về HTTP 200, các requests sau trả về HTTP 429 (Too Many Requests).

### Test 3: Kiểm tra Redis rate limit keys

```bash
docker exec -it redis redis-cli

# Xem rate limit keys
KEYS request_rate_limiter*

# Xem chi tiết một key
GET "request_rate_limiter.{user-id-xxx}.tokens"
GET "request_rate_limiter.{user-id-xxx}.timestamp"
```

### Test 4: Rate limit theo IP cho unauthenticated requests

```bash
for i in {1..25}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8888/api/identity/auth/token \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}')
  echo "Request $i: HTTP $STATUS"
done
```

Mong đợi: sau khoảng 20 requests, nhận HTTP 429.

### Test 5: Frontend WebSocket STOMP

**Bước 1:** Mở browser, truy cập frontend `http://localhost:3000`.

**Bước 2:** Đăng nhập → mở Developer Console (F12 → Console tab).

**Bước 3:** Kiểm tra log: `STOMP connected` xuất hiện.

**Bước 4:** Từ một tab khác, gửi một notification hoặc chat message.

**Bước 5:** Kiểm tra tab đầu tiên → notification hiển thị realtime.

### Test 6: Moderation Status hiển thị

**Bước 1:** Tạo một post mới.

```bash
curl -X POST http://localhost:8888/api/content/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"content": "Test moderation status"}'
```

**Bước 2:** Xem post trên frontend → thấy badge "Pending Review" (màu vàng).

**Bước 3:** Sau khi AI moderation xử lý xong → badge chuyển thành:
- "Approved" (màu xanh) nếu nội dung hợp lệ
- "Rejected" (màu đỏ) nếu nội dung vi phạm

### Test 7: Đợi rate limit reset

```bash
# Gửi requests vượt ngưỡng
for i in {1..25}; do
  curl -s -o /dev/null -w "" http://localhost:8888/api/content/posts \
    -H "Authorization: Bearer <TOKEN>"
done

# Đợi 1 giây (replenishRate = 10/s → tokens được bổ sung)
sleep 1

# Gửi lại → thành công
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8888/api/content/posts \
  -H "Authorization: Bearer <TOKEN>")
echo "After reset: HTTP $STATUS"
```

Mong đợi: HTTP 200 (tokens đã được bổ sung).

## Checklist

- [ ] Thêm spring-boot-starter-data-redis-reactive vào api-gateway
- [ ] Cấu hình RequestRateLimiter trong application.yaml
- [ ] Tạo RateLimiterConfig với UserKeyResolver
- [ ] Cấu hình Redis connection cho api-gateway
- [ ] Frontend: cài đặt sockjs-client và @stomp/stompjs
- [ ] Frontend: tạo websocket.ts với createStompClient
- [ ] Frontend: tạo useNotifications hook
- [ ] Frontend: tạo PostCard component với ModerationBadge
- [ ] Test: gửi 10 requests/s → tất cả HTTP 200
- [ ] Test: gửi 30 requests/s → requests vượt ngưỡng nhận HTTP 429
- [ ] Test: kiểm tra Redis rate limit keys
- [ ] Test: rate limit theo IP cho unauthenticated requests
- [ ] Test: STOMP WebSocket connect thành công trên frontend
- [ ] Test: notification realtime hoạt động
- [ ] Test: moderation badge hiển thị đúng trạng thái
- [ ] Test: rate limit reset sau 1 giây
