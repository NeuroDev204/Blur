# 🔄 BLUR BACKEND MIGRATION GUIDE

## Hướng dẫn chi tiết tái cấu trúc Backend

> **Mục tiêu**: Refactor blur-common-lib, chuyển JWT sang Cookie, thống nhất WebSocket, tích hợp Kafka EDA
> **Thời gian ước tính**: 4-6 tuần

---

# 📊 TỔNG QUAN THAY ĐỔI

## So sánh Trước vs Sau

| Thành phần                | TRƯỚC                 | SAU                      | Lợi ích             |
|---------------------------|-----------------------|--------------------------|---------------------|
| **Common Classes**        | Duplicate 6+ services | blur-common-lib duy nhất | -70% code duplicate |
| **JWT Storage**           | localStorage          | HttpOnly Cookie          | Bảo mật XSS         |
| **Real-time**             | Socket.IO + STOMP     | WebSocket STOMP duy nhất | Giảm complexity     |
| **Service Communication** | REST-heavy            | Kafka Event-Driven       | Loose coupling      |

---

# 📦 PHẦN 1: BLUR-COMMON-LIB REFACTORING

## 1.1 Các class cần chuyển vào common-lib

### Classes hiện đang bị duplicate:

| Class                              | Services đang có | Trạng thái               |
|------------------------------------|------------------|--------------------------|
| `CustomJwtDecoder`                 | 6 services       | ❌ Cần gộp                |
| `JWTAuthenticationEntryPoint`      | 6 services       | ❌ Cần gộp                |
| `AuthenticationRequestInterceptor` | 5 services       | ❌ Cần gộp                |
| `ApiResponse`                      | 4 services       | ✅ Đã có trong common-lib |
| `GlobalExceptionHandler`           | 4 services       | ⚠️ Có nhưng cần update   |
| `AppException/BlurException`       | 4 services       | ⚠️ Có nhưng cần update   |
| `ErrorCode`                        | 4 services       | ❌ Cần gộp tất cả         |

### Cấu trúc thư mục mới cho blur-common-lib:

```
blur-common-lib/src/main/java/com/blur/common/
├── configuration/
│   ├── CustomJwtDecoder.java          # [NEW]
│   ├── JWTAuthenticationEntryPoint.java  # [NEW]
│   ├── AuthenticationRequestInterceptor.java  # [NEW]
│   ├── BaseSecurityConfig.java        # [NEW] Abstract class
│   └── RedisConfig.java               # [NEW] Shared Redis config
├── dto/
│   ├── request/
│   │   └── IntrospectRequest.java     # [EXISTS]
│   └── response/
│       ├── ApiResponse.java           # [EXISTS]
│       ├── UserResponse.java          # [EXISTS]
│       └── UserProfileResponse.java   # [EXISTS]
├── exception/
│   ├── BlurException.java             # [EXISTS]
│   ├── ErrorCode.java                 # [UPDATE] Gộp tất cả error codes
│   └── GlobalExceptionHandler.java    # [EXISTS]
├── event/                             # [NEW] Kafka events
│   ├── BaseEvent.java
│   ├── CommentCreatedEvent.java
│   ├── PostCreatedEvent.java
│   └── ...
└── security/                          # [NEW]
    └── SecurityUtils.java             # Common security utilities
```

---

## 1.2 Chi tiết Implementation

### 1.2.1 ErrorCode hợp nhất

**File:** `blur-common-lib/.../exception/ErrorCode.java`

```java
package com.blur.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
  // ============= COMMON ERRORS (1000-1999) =============
  UNCATEGORIZED_EXCEPTION(1001, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_KEY(1002, "Invalid message key", HttpStatus.BAD_REQUEST),
  UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
  INVALID_TOKEN(1005, "Invalid token", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED(1006, "Token has expired", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED(1007, "Access denied", HttpStatus.FORBIDDEN),

  // ============= USER ERRORS (2000-2999) =============
  USER_EXISTED(2001, "User already exists", HttpStatus.BAD_REQUEST),
  USER_NOT_EXISTED(2002, "User does not exist", HttpStatus.NOT_FOUND),
  INVALID_USERNAME_OR_PASSWORD(2003, "Invalid username or password", HttpStatus.BAD_REQUEST),
  USERNAME_INVALID(2004, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
  PASSWORD_INVALID(2005, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
  EMAIL_INVALID(2006, "Invalid email format", HttpStatus.BAD_REQUEST),

  // ============= CHAT ERRORS (3000-3999) =============
  CONVERSATION_NOT_FOUND(3001, "Conversation not found", HttpStatus.NOT_FOUND),
  MESSAGE_NOT_FOUND(3002, "Message not found", HttpStatus.NOT_FOUND),
  EMPTY_MESSAGE(3003, "Message cannot be empty", HttpStatus.BAD_REQUEST),
  DUPLICATE_MESSAGE(3004, "Duplicate message detected", HttpStatus.BAD_REQUEST),
  CONVERSATION_ID_REQUIRED(3005, "Conversation ID is required", HttpStatus.BAD_REQUEST),
  RECEIVER_NOT_FOUND(3006, "Receiver not found", HttpStatus.NOT_FOUND),
  INVALID_CONVERSATION(3007, "Invalid conversation", HttpStatus.BAD_REQUEST),
  SESSION_EXPIRED(3008, "Session expired", HttpStatus.UNAUTHORIZED),

  // ============= CALL ERRORS (3500-3599) =============
  CALL_NOT_FOUND(3501, "Call not found", HttpStatus.NOT_FOUND),
  CALL_INITIATE_FAILED(3502, "Failed to initiate call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_ANSWER_FAILED(3503, "Failed to answer call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_REJECT_FAILED(3504, "Failed to reject call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_END_FAILED(3505, "Failed to end call", HttpStatus.INTERNAL_SERVER_ERROR),
  USER_NOT_AVAILABLE(3506, "User is not available", HttpStatus.BAD_REQUEST),
  USER_OFFLINE(3507, "User is offline", HttpStatus.BAD_REQUEST),
  INVALID_CALL_TYPE(3508, "Invalid call type", HttpStatus.BAD_REQUEST),
  PEER_NOT_FOUND(3509, "Peer not found", HttpStatus.NOT_FOUND),

  // ============= POST ERRORS (4000-4999) =============
  POST_NOT_FOUND(4001, "Post not found", HttpStatus.NOT_FOUND),
  POST_CREATE_FAILED(4002, "Failed to create post", HttpStatus.INTERNAL_SERVER_ERROR),
  COMMENT_NOT_FOUND(4003, "Comment not found", HttpStatus.NOT_FOUND),
  ALREADY_LIKED(4004, "Already liked this post", HttpStatus.BAD_REQUEST),
  NOT_LIKED_YET(4005, "Not liked this post yet", HttpStatus.BAD_REQUEST),

  // ============= STORY ERRORS (5000-5999) =============
  STORY_NOT_FOUND(5001, "Story not found", HttpStatus.NOT_FOUND),
  STORY_EXPIRED(5002, "Story has expired", HttpStatus.GONE),

  // ============= NOTIFICATION ERRORS (6000-6999) =============
  NOTIFICATION_NOT_FOUND(6001, "Notification not found", HttpStatus.NOT_FOUND),
  NOTIFICATION_SEND_FAILED(6002, "Failed to send notification", HttpStatus.INTERNAL_SERVER_ERROR),

  // ============= PROFILE ERRORS (7000-7999) =============
  PROFILE_NOT_FOUND(7001, "Profile not found", HttpStatus.NOT_FOUND),
  ALREADY_FOLLOWING(7002, "Already following this user", HttpStatus.BAD_REQUEST),
  NOT_FOLLOWING(7003, "Not following this user", HttpStatus.BAD_REQUEST),

  // ============= WEBSOCKET ERRORS (8000-8999) =============
  TOKEN_REQUIRED(8001, "Token is required", HttpStatus.UNAUTHORIZED),
  AUTH_FAILED(8002, "Authentication failed", HttpStatus.UNAUTHORIZED),
  DISCONNECT_FAILED(8003, "Disconnect failed", HttpStatus.INTERNAL_SERVER_ERROR),
  MESSAGE_SEND_FAILED(8004, "Message send failed", HttpStatus.INTERNAL_SERVER_ERROR),
  SEND_EVENT_FAILED(8005, "Send event failed", HttpStatus.INTERNAL_SERVER_ERROR),
  WEBRTC_OFFER_FAILED(8006, "WebRTC offer failed", HttpStatus.INTERNAL_SERVER_ERROR),
  WEBRTC_ANSWER_FAILED(8007, "WebRTC answer failed", HttpStatus.INTERNAL_SERVER_ERROR),
  ICE_CANDIDATE_FAILED(8008, "ICE candidate failed", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatusCode statusCode;

  ErrorCode(int code, String message, HttpStatusCode statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
  }
}
```

### 1.2.2 CustomJwtDecoder chung

**File:** `blur-common-lib/.../configuration/CustomJwtDecoder.java`

```java
package com.blur.common.configuration;

import java.text.ParseException;
import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import com.blur.common.dto.request.IntrospectRequest;
import com.blur.common.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;

@Component
public class CustomJwtDecoder implements JwtDecoder {

  @Value("${jwt.signerKey}")
  private String signerKey;

  private NimbusJwtDecoder nimbusJwtDecoder = null;

  // Tùy chọn: inject IdentityClient để introspect token
  // Nếu không cần introspect, có thể bỏ qua

  @Override
  public Jwt decode(String token) throws JwtException {
    // Có thể thêm logic introspect ở đây nếu cần

    if (Objects.isNull(nimbusJwtDecoder)) {
      SecretKeySpec key = new SecretKeySpec(signerKey.getBytes(), "HS512");
      nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(key)
          .macAlgorithm(MacAlgorithm.HS512)
          .build();
    }
    return nimbusJwtDecoder.decode(token);
  }
}
```

### 1.2.3 JWTAuthenticationEntryPoint chung

**File:** `blur-common-lib/.../configuration/JWTAuthenticationEntryPoint.java`

```java
package com.blur.common.configuration;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) throws IOException, ServletException {

    ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

    response.setStatus(errorCode.getStatusCode().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiResponse<?> apiResponse = ApiResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();

    ObjectMapper objectMapper = new ObjectMapper();
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.flushBuffer();
  }
}
```

---

## 1.3 Hướng dẫn từng bước

### Bước 1: Update blur-common-lib

```bash
cd Backend/blur-common-lib
```

1. Tạo các package mới: `configuration/`, `security/`, `event/`
2. Copy và merge các class từ các service vào common-lib
3. Update `pom.xml` để thêm dependencies cần thiết:

```xml

<dependencies>
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Spring OAuth2 Resource Server -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Feign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

4. Build và install:

```bash
mvn clean install
```

### Bước 2: Update từng Service

Với mỗi service (chat-service, post-service, notification-service, story-service, profile-service):

1. **Xóa các class duplicate:**
    - `CustomJwtDecoder.java`
    - `JWTAuthenticationEntryPoint.java`
    - `AuthenticationRequestInterceptor.java`
    - `AppException.java` (sử dụng `BlurException`)
    - `ErrorCode.java` (class local)
    - `GlobalExceptionHandler.java`
    - `ApiResponse.java` (nếu có local)

2. **Update imports:**

```java
// TRƯỚC

import com.blur.chatservice.exception.AppException;
import com.blur.chatservice.exception.ErrorCode;

// SAU
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
```

3. **Update SecurityConfig:**

```java
import com.blur.common.configuration.CustomJwtDecoder;
import com.blur.common.configuration.JWTAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final CustomJwtDecoder customJwtDecoder;

  // ... existing code
}
```

---

## 1.4 Lợi ích vs Rủi ro

### ✅ Lợi ích

| Lợi ích                    | Mô tả                                     |
|----------------------------|-------------------------------------------|
| **Giảm duplicate code**    | ~70% reduction, dễ maintain               |
| **Single source of truth** | ErrorCode, Exception xử lý nhất quán      |
| **Dễ update**              | Sửa 1 chỗ, áp dụng tất cả services        |
| **Consistency**            | Response format, error handling đồng nhất |

### ⚠️ Rủi ro và Cách giảm thiểu

| Rủi ro           | Mức độ     | Cách giảm thiểu                     |
|------------------|------------|-------------------------------------|
| Breaking changes | Cao        | Test kỹ từng service sau khi update |
| Version conflict | Trung bình | Semantic versioning cho common-lib  |
| Tight coupling   | Thấp       | Keep common-lib minimal             |

---

# 🍪 PHẦN 2: JWT SANG HTTPONLY COOKIE

## 2.1 Tổng quan thay đổi

### Kiến trúc hiện tại vs Kiến trúc mới

```
HIỆN TẠI (localStorage)
========================
┌─────────┐    POST /login     ┌─────────────┐
│ Browser │ ─────────────────► │  Identity   │
│         │ ◄───────────────── │  Service    │
└────┬────┘   { token: "..." } └─────────────┘
     │
     │ localStorage.setItem("token", token)
     │
     ▼
┌─────────┐    Authorization: Bearer <token>
│ Browser │ ──────────────────────────────────►
└─────────┘

SAU (HttpOnly Cookie)
=====================
┌─────────┐    POST /login     ┌─────────────┐
│ Browser │ ─────────────────► │  Identity   │
│         │ ◄───────────────── │  Service    │
└────┬────┘   Set-Cookie:      └─────────────┘
     │        access_token=...;
     │        HttpOnly; Secure; SameSite=Strict
     ▼
┌─────────┐    Cookie: access_token=...
│ Browser │ ──────────────────────────────────►
└─────────┘    (tự động gửi bởi browser)
```

## 2.2 Backend Changes

### 2.2.1 IdentityService - AuthController

**File:** `IdentityService/.../controller/AuthController.java`

```java
package org.identityservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
// ... other imports

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationService authenticationService;

  @Value("${jwt.valid-duration}")
  private long validDuration;

  @Value("${app.cookie.secure:true}")
  private boolean cookieSecure;

  @Value("${app.cookie.domain:}")
  private String cookieDomain;

  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(
      @RequestBody AuthRequest request,
      HttpServletResponse response) {

    AuthResponse authResponse = authenticationService.authenticate(request);

    // Set access token cookie
    Cookie accessCookie = new Cookie("access_token", authResponse.getToken());
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(cookieSecure); // true in production
    accessCookie.setPath("/");
    accessCookie.setMaxAge((int) validDuration);
    // SameSite=Strict (Spring Boot 2.6+)
    accessCookie.setAttribute("SameSite", "Strict");

    if (!cookieDomain.isEmpty()) {
      accessCookie.setDomain(cookieDomain);
    }

    response.addCookie(accessCookie);

    // Return response without token in body (optional, for backward compatibility)
    return ApiResponse.<AuthResponse>builder()
        .result(AuthResponse.builder()
            .authenticated(true)
            .build())
        .build();
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @CookieValue(name = "access_token", required = false) String token,
      HttpServletResponse response) {

    if (token != null) {
      authenticationService.logout(LogoutRequest.builder().token(token).build());
    }

    // Clear cookie
    Cookie cookie = new Cookie("access_token", null);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(0); // Delete cookie
    response.addCookie(cookie);

    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/refresh")
  public ApiResponse<Void> refreshToken(
      @CookieValue(name = "access_token") String token,
      HttpServletResponse response) throws ParseException, JOSEException {

    AuthResponse authResponse = authenticationService.refreshToken(
        RefreshRequest.builder().token(token).build());

    // Set new cookie
    Cookie accessCookie = new Cookie("access_token", authResponse.getToken());
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(cookieSecure);
    accessCookie.setPath("/");
    accessCookie.setMaxAge((int) validDuration);
    accessCookie.setAttribute("SameSite", "Strict");

    response.addCookie(accessCookie);

    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/introspect")
  public ApiResponse<IntrospecResponse> introspect(
      @CookieValue(name = "access_token") String token)
      throws ParseException, JOSEException {

    return ApiResponse.<IntrospecResponse>builder()
        .result(authenticationService.introspect(
            IntrospectRequest.builder().token(token).build()))
        .build();
  }
}
```

### 2.2.2 API Gateway - Extract Cookie to Header

**File:** `api-gateway/.../configuration/CookieToHeaderFilter.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CookieToHeaderFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    HttpCookie cookie = exchange.getRequest().getCookies().getFirst("access_token");

    if (cookie != null) {
      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
          .header("Authorization", "Bearer " + cookie.getValue())
          .build();

      return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return -100; // Run before AuthenticationFilter
  }
}
```

### 2.2.3 Other Services - SecurityConfig Update

Các service khác giữ nguyên cách đọc từ Authorization header vì API Gateway đã convert cookie thành header.

---

## 2.3 Frontend Changes

### 2.3.1 Remove LocalStorageService usage

**File:** `frontend/src/service/LocalStorageService.ts` - **XÓA FILE NÀY**

### 2.3.2 Update API Configuration

**File:** `frontend/src/service/httpClient.ts`

```typescript
import axios from 'axios';

const httpClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8888',
    withCredentials: true, // ⚠️ QUAN TRỌNG: Gửi cookies với mỗi request
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor - không cần thêm token thủ công nữa
httpClient.interceptors.request.use(
    (config) => {
        // Cookie sẽ được browser tự động gửi
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor - handle 401
httpClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (error.response?.status === 401) {
            // Try to refresh token
            try {
                await axios.post('/api/auth/refresh', {}, {withCredentials: true});
                // Retry original request
                return httpClient(error.config);
            } catch (refreshError) {
                // Redirect to login
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default httpClient;
```

### 2.3.3 Update Auth API

**File:** `frontend/src/api/authAPI.ts`

```typescript
import httpClient from '../service/httpClient';

export const login = async (username: string, password: string) => {
    const response = await httpClient.post('/api/auth/login', {username, password});
    // Cookie được set tự động bởi browser
    return response.data;
};

export const logout = async () => {
    await httpClient.post('/api/auth/logout');
    // Cookie được xóa bởi server
    window.location.href = '/login';
};

export const refreshToken = async () => {
    await httpClient.post('/api/auth/refresh');
};

// Check if user is authenticated
export const checkAuth = async (): Promise<boolean> => {
    try {
        const response = await httpClient.post('/api/auth/introspect');
        return response.data.result?.valid === true;
    } catch {
        return false;
    }
};
```

### 2.3.4 Update JwtService

**File:** `frontend/src/service/JwtService.ts`

```typescript
import httpClient from './httpClient';

interface UserDetails {
    userId?: string;
    email?: string;
    name?: string;
}

// Không thể decode JWT từ HttpOnly cookie ở client-side
// Cần gọi API để lấy thông tin user
export const getUserDetails = async (): Promise<UserDetails | null> => {
    try {
        const response = await httpClient.get('/api/users/my-info');
        return response.data.result;
    } catch (error) {
        console.log('Failed to get user info', error);
        return null;
    }
};
```

### 2.3.5 WebSocket Connection với Cookie

**File:** `frontend/src/contexts/SocketContext.js`

```javascript
import {io} from 'socket.io-client';

// Socket.IO sẽ tự động gửi cookie khi kết nối
const socket = io(SOCKET_URL, {
    withCredentials: true, // Gửi cookie
    // Không cần auth: { token } nữa
});
```

**File:** `frontend/src/service/notificationSocket.ts`

```typescript
import SockJS from 'sockjs-client';
import {Client} from '@stomp/stompjs';

const createNotificationSocket = () => {
    const client = new Client({
        webSocketFactory: () => new SockJS('/ws-notification', null, {
            // SockJS sẽ tự gửi cookie
        }),
        // Không cần connectHeaders với token
    });
    return client;
};
```

---

## 2.4 Lợi ích vs Rủi ro

### ✅ Lợi ích

| Lợi ích             | Mô tả                                          |
|---------------------|------------------------------------------------|
| **XSS Protection**  | JavaScript không thể đọc HttpOnly cookie       |
| **Tự động gửi**     | Browser tự gửi cookie, không cần code thủ công |
| **CSRF Protection** | SameSite=Strict ngăn CSRF attacks              |
| **Secure flag**     | Cookie chỉ gửi qua HTTPS                       |

### ⚠️ Rủi ro và Cách giảm thiểu

| Rủi ro              | Mức độ     | Cách giảm thiểu                           |
|---------------------|------------|-------------------------------------------|
| CSRF attacks        | Trung bình | SameSite=Strict, CSRF token cho mutations |
| Cookie size limit   | Thấp       | JWT payload nhỏ gọn                       |
| Cross-domain issues | Thấp       | Same origin (không cần CORS phức tạp)     |

---

# 🔌 PHẦN 3: THỐNG NHẤT WEBSOCKET

## 3.1 Phân tích hiện trạng

### Chat Service (Socket.IO)

- **Library:** `netty-socketio`
- **Port:** 8099
- **Features:** Chat messages, Voice/Video calls, WebRTC signaling

### Notification Service (STOMP WebSocket)

- **Library:** Spring WebSocket
- **Endpoint:** `/ws-notification`
- **Features:** Push notifications (like, comment, follow...)

## 3.2 Recommendation: Tạo Realtime Gateway Service

> **Khuyến nghị:** Tạo một service mới `realtime-gateway` để xử lý tất cả WebSocket connections.

### Lý do:

1. **Single connection point** - Client chỉ cần 1 WebSocket connection
2. **Shared authentication** - Xác thực một lần
3. **Easier scaling** - Scale độc lập với business services
4. **Cleaner architecture** - Separation of concerns

### Kiến trúc mới:

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ WebSocket (STOMP)
                             │ /ws
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   REALTIME-GATEWAY SERVICE                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  WebSocket Handler (Spring STOMP)                         │  │
│  │  - Authentication via Cookie                              │  │
│  │  - User session management (Redis)                        │  │
│  │  - Message routing                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│                             │                                    │
│        ┌────────────────────┼────────────────────┐              │
│        │                    │                    │              │
│        ▼                    ▼                    ▼              │
│  ┌──────────┐        ┌──────────┐        ┌──────────┐          │
│  │   Chat   │        │  Notif   │        │  WebRTC  │          │
│  │ Handler  │        │ Handler  │        │ Handler  │          │
│  └────┬─────┘        └────┬─────┘        └────┬─────┘          │
└───────┼───────────────────┼───────────────────┼─────────────────┘
        │                   │                   │
        │ Kafka             │ Kafka             │ Direct
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Chat Service │    │ Notification │    │ Redis        │
│ (Business)   │    │ Service      │    │ (Signaling)  │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## 3.3 Implementation: Realtime Gateway

### 3.3.1 Tạo service mới

```bash
cd Backend
mkdir realtime-gateway
cd realtime-gateway
```

**pom.xml:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.blur</groupId>
    <artifactId>realtime-gateway</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Spring WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Common Lib -->
        <dependency>
            <groupId>com.blur</groupId>
            <artifactId>blur-common-lib</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

### 3.3.2 WebSocket Configuration

**File:** `realtime-gateway/.../configuration/WebSocketConfig.java`

```java
package com.blur.realtimegateway.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtChannelInterceptor jwtChannelInterceptor;
  private final CookieHandshakeInterceptor cookieHandshakeInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // Destinations cho client subscribe
    registry.enableSimpleBroker(
        "/topic",           // Public topics (e.g., /topic/chat/{conversationId})
        "/queue"            // Private queues (e.g., /queue/notifications)
    );

    // Prefix cho client gửi message lên server
    registry.setApplicationDestinationPrefixes("/app");

    // User-specific destinations
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .addInterceptors(cookieHandshakeInterceptor)
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(jwtChannelInterceptor);
  }
}
```

### 3.3.3 Cookie Authentication

**File:** `realtime-gateway/.../configuration/CookieHandshakeInterceptor.java`

```java
package com.blur.realtimegateway.configuration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CookieHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtValidator jwtValidator;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {

    if (request instanceof ServletServerHttpRequest servletRequest) {
      HttpServletRequest httpRequest = servletRequest.getServletRequest();
      Cookie[] cookies = httpRequest.getCookies();

      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("access_token".equals(cookie.getName())) {
            String token = cookie.getValue();

            try {
              String userId = jwtValidator.validateAndGetUserId(token);
              attributes.put("userId", userId);
              attributes.put("token", token);
              log.info("✅ WebSocket handshake authenticated for user: {}", userId);
              return true;
            } catch (Exception e) {
              log.warn("❌ Invalid token in WebSocket handshake");
              return false;
            }
          }
        }
      }
    }

    log.warn("❌ No access_token cookie found");
    return false;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
    // No action needed
  }
}
```

### 3.3.4 Message Handlers

**File:** `realtime-gateway/.../handler/ChatMessageHandler.java`

```java
package com.blur.realtimegateway.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageHandler {

  private final SimpMessagingTemplate messagingTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final RedisSessionService sessionService;

  /**
   * Client gửi tin nhắn: /app/chat/send
   */
  @MessageMapping("/chat/send")
  public void handleSendMessage(@Payload ChatMessageRequest request,
                                Principal principal,
                                @Header("simpSessionId") String sessionId) {

    String senderId = principal.getName();
    log.info("📨 Message from {} in conversation {}", senderId, request.getConversationId());

    // 1. Gửi qua Kafka để chat-service xử lý & lưu DB
    kafkaTemplate.send("chat.message.send", request.getConversationId(),
        toJson(request, senderId));

    // 2. Gửi ngay cho người nhận (nếu online)
    String receiverId = request.getReceiverId();
    if (sessionService.isUserOnline(receiverId)) {
      messagingTemplate.convertAndSendToUser(
          receiverId,
          "/queue/messages",
          buildMessagePayload(request, senderId)
      );
    }

    // 3. Gửi xác nhận cho người gửi
    messagingTemplate.convertAndSendToUser(
        senderId,
        "/queue/message-sent",
        Map.of("tempId", request.getTempMessageId(), "status", "SENT")
    );
  }

  /**
   * Client đánh dấu đã đọc: /app/chat/read
   */
  @MessageMapping("/chat/read")
  public void handleMarkAsRead(@Payload MarkAsReadRequest request, Principal principal) {
    kafkaTemplate.send("chat.message.read", request.getConversationId(),
        toJson(request, principal.getName()));
  }

  /**
   * Typing indicator: /app/chat/typing
   */
  @MessageMapping("/chat/typing")
  public void handleTyping(@Payload TypingRequest request, Principal principal) {
    String senderId = principal.getName();
    String receiverId = request.getReceiverId();

    if (sessionService.isUserOnline(receiverId)) {
      messagingTemplate.convertAndSendToUser(
          receiverId,
          "/queue/typing",
          Map.of("conversationId", request.getConversationId(),
              "userId", senderId, "isTyping", request.isTyping())
      );
    }
  }
}
```

**File:** `realtime-gateway/.../handler/WebRTCHandler.java`

```java
package com.blur.realtimegateway.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRTCHandler {

  private final SimpMessagingTemplate messagingTemplate;
  private final RedisSessionService sessionService;
  private final CallSessionService callSessionService;

  /**
   * Initiate call: /app/call/initiate
   */
  @MessageMapping("/call/initiate")
  public void handleCallInitiate(@Payload CallInitiateRequest request, Principal principal) {
    String callerId = principal.getName();
    String receiverId = request.getReceiverId();

    log.info("📞 Call initiated from {} to {}", callerId, receiverId);

    // Check if receiver is online
    if (!sessionService.isUserOnline(receiverId)) {
      messagingTemplate.convertAndSendToUser(
          callerId, "/queue/call-failed",
          Map.of("reason", "USER_OFFLINE")
      );
      return;
    }

    // Create call session
    String callId = callSessionService.createCall(callerId, receiverId, request.getCallType());

    // Notify receiver
    messagingTemplate.convertAndSendToUser(
        receiverId, "/queue/call-incoming",
        Map.of("callId", callId, "callerId", callerId,
            "callerName", request.getCallerName(),
            "callType", request.getCallType())
    );

    // Confirm to caller
    messagingTemplate.convertAndSendToUser(
        callerId, "/queue/call-initiated",
        Map.of("callId", callId)
    );
  }

  /**
   * WebRTC Offer: /app/webrtc/offer
   */
  @MessageMapping("/webrtc/offer")
  public void handleOffer(@Payload WebRTCOffer offer, Principal principal) {
    messagingTemplate.convertAndSendToUser(
        offer.getTo(), "/queue/webrtc-offer",
        Map.of("from", principal.getName(), "sdp", offer.getSdp())
    );
  }

  /**
   * WebRTC Answer: /app/webrtc/answer
   */
  @MessageMapping("/webrtc/answer")
  public void handleAnswer(@Payload WebRTCAnswer answer, Principal principal) {
    messagingTemplate.convertAndSendToUser(
        answer.getTo(), "/queue/webrtc-answer",
        Map.of("from", principal.getName(), "sdp", answer.getSdp())
    );
  }

  /**
   * ICE Candidate: /app/webrtc/ice
   */
  @MessageMapping("/webrtc/ice")
  public void handleIceCandidate(@Payload ICECandidate candidate, Principal principal) {
    messagingTemplate.convertAndSendToUser(
        candidate.getTo(), "/queue/webrtc-ice",
        Map.of("from", principal.getName(), "candidate", candidate.getCandidate())
    );
  }
}
```

### 3.3.5 Kafka Consumer (nhận events từ các service khác)

**File:** `realtime-gateway/.../kafka/NotificationConsumer.java`

```java
package com.blur.realtimegateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

  private final SimpMessagingTemplate messagingTemplate;

  @KafkaListener(topics = "notification.push", groupId = "realtime-gateway")
  public void handleNotification(String message) {
    NotificationEvent event = parseEvent(message);

    log.info("📢 Pushing notification to user: {}", event.getUserId());

    messagingTemplate.convertAndSendToUser(
        event.getUserId(),
        "/queue/notifications",
        event.getPayload()
    );
  }

  @KafkaListener(topics = "chat.message.saved", groupId = "realtime-gateway")
  public void handleMessageSaved(String message) {
    // Khi chat-service đã lưu message thành công
    // Có thể gửi xác nhận hoặc update status
  }
}
```

---

## 3.4 Frontend Updates

### 3.4.1 Unified WebSocket Client

**File:** `frontend/src/service/websocketService.ts`

```typescript
import SockJS from 'sockjs-client';
import {Client, IMessage, StompSubscription} from '@stomp/stompjs';

class WebSocketService {
    private client: Client | null = null;
    private subscriptions: Map<string, StompSubscription> = new Map();
    private connectionPromise: Promise<void> | null = null;

    connect(): Promise<void> {
        if (this.connectionPromise) {
            return this.connectionPromise;
        }

        this.connectionPromise = new Promise((resolve, reject) => {
            this.client = new Client({
                webSocketFactory: () => new SockJS('/ws'),
                // Cookie sẽ được gửi tự động

                onConnect: () => {
                    console.log('✅ WebSocket connected');
                    resolve();
                },

                onStompError: (frame) => {
                    console.error('❌ STOMP error:', frame);
                    reject(new Error(frame.body));
                },

                onDisconnect: () => {
                    console.log('🔌 WebSocket disconnected');
                    this.connectionPromise = null;
                },

                reconnectDelay: 5000,
            });

            this.client.activate();
        });

        return this.connectionPromise;
    }

    // ============ CHAT ============

    subscribeToMessages(callback: (message: any) => void): void {
        this.subscribe('/user/queue/messages', callback);
    }

    subscribeToMessageSent(callback: (confirmation: any) => void): void {
        this.subscribe('/user/queue/message-sent', callback);
    }

    subscribeToTyping(callback: (data: any) => void): void {
        this.subscribe('/user/queue/typing', callback);
    }

    sendMessage(conversationId: string, message: string, receiverId: string, tempId: string): void {
        this.send('/app/chat/send', {
            conversationId,
            message,
            receiverId,
            tempMessageId: tempId
        });
    }

    sendTyping(conversationId: string, receiverId: string, isTyping: boolean): void {
        this.send('/app/chat/typing', {conversationId, receiverId, isTyping});
    }

    // ============ CALLS ============

    subscribeToIncomingCall(callback: (call: any) => void): void {
        this.subscribe('/user/queue/call-incoming', callback);
    }

    subscribeToCallInitiated(callback: (data: any) => void): void {
        this.subscribe('/user/queue/call-initiated', callback);
    }

    subscribeToWebRTCOffer(callback: (offer: any) => void): void {
        this.subscribe('/user/queue/webrtc-offer', callback);
    }

    subscribeToWebRTCAnswer(callback: (answer: any) => void): void {
        this.subscribe('/user/queue/webrtc-answer', callback);
    }

    subscribeToICECandidate(callback: (candidate: any) => void): void {
        this.subscribe('/user/queue/webrtc-ice', callback);
    }

    initiateCall(receiverId: string, callerName: string, callType: 'VOICE' | 'VIDEO'): void {
        this.send('/app/call/initiate', {receiverId, callerName, callType});
    }

    sendWebRTCOffer(to: string, sdp: string): void {
        this.send('/app/webrtc/offer', {to, sdp});
    }

    sendWebRTCAnswer(to: string, sdp: string): void {
        this.send('/app/webrtc/answer', {to, sdp});
    }

    sendICECandidate(to: string, candidate: any): void {
        this.send('/app/webrtc/ice', {to, candidate});
    }

    // ============ NOTIFICATIONS ============

    subscribeToNotifications(callback: (notification: any) => void): void {
        this.subscribe('/user/queue/notifications', callback);
    }

    // ============ INTERNAL ============

    private subscribe(destination: string, callback: (message: any) => void): void {
        if (!this.client || !this.client.connected) {
            throw new Error('WebSocket not connected');
        }

        const subscription = this.client.subscribe(destination, (msg: IMessage) => {
            callback(JSON.parse(msg.body));
        });

        this.subscriptions.set(destination, subscription);
    }

    private send(destination: string, body: any): void {
        if (!this.client || !this.client.connected) {
            throw new Error('WebSocket not connected');
        }
        this.client.publish({destination, body: JSON.stringify(body)});
    }

    disconnect(): void {
        this.subscriptions.forEach(sub => sub.unsubscribe());
        this.subscriptions.clear();
        this.client?.deactivate();
        this.client = null;
        this.connectionPromise = null;
    }
}

export const websocketService = new WebSocketService();
```

---

## 3.5 Migration từ Chat Service

### Những gì cần giữ lại trong chat-service:

1. **Business logic** - ChatMessageService, ConversationService, CallService
2. **Database operations** - Repositories
3. **REST APIs** - Get messages, get conversations, etc.

### Những gì chuyển sang realtime-gateway:

1. **SocketHandler.java** → Convert sang STOMP handlers
2. **WebSocket session management** → Redis-based
3. **Real-time message delivery** → STOMP messaging

### Chat Service sẽ:

1. Nhận events từ Kafka (từ realtime-gateway)
2. Xử lý business logic, lưu DB
3. Publish events khi hoàn thành (chat.message.saved)

---

## 3.6 Lợi ích vs Rủi ro

### ✅ Lợi ích

| Lợi ích                | Mô tả                           |
|------------------------|---------------------------------|
| **Single connection**  | Client chỉ cần 1 WebSocket      |
| **Standard protocol**  | STOMP là industry standard      |
| **Spring integration** | Native Spring WebSocket support |
| **Easier testing**     | STOMP có nhiều testing tools    |
| **Scalability**        | Dễ scale với message broker     |

### ⚠️ Rủi ro

| Rủi ro           | Mức độ     | Cách giảm thiểu                |
|------------------|------------|--------------------------------|
| WebRTC migration | Cao        | Test kỹ, giữ fallback 2 tuần   |
| Breaking changes | Cao        | Versioned API, gradual rollout |
| Learning curve   | Trung bình | Documentation rõ ràng          |

---

# 📡 PHẦN 4: KAFKA EVENT-DRIVEN (Theo EDA-MIGRATION-ROADMAP.md)

## 4.1 Kafka Topics cần tạo thêm

```bash
# Chat events
docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat.message.send --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat.message.saved --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat.message.read --partitions 3 --replication-factor 1

# Notification events  
docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic notification.push --partitions 3 --replication-factor 1

# Call events
docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic call.initiated --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic call.ended --partitions 3 --replication-factor 1
```

## 4.2 Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         EVENT FLOW                               │
└─────────────────────────────────────────────────────────────────┘

1. CHAT MESSAGE FLOW
   
   Client ──► Realtime Gateway ──► Kafka (chat.message.send)
                    │                         │
                    │                         ▼
                    │                 ┌──────────────┐
                    │                 │ Chat Service │
                    │                 │  - Validate  │
                    │                 │  - Save DB   │
                    │                 └──────┬───────┘
                    │                        │
                    │                        ▼
                    │                 Kafka (chat.message.saved)
                    │                        │
                    ▼                        ▼
              Direct Push           Realtime Gateway
           (if receiver online)     (Update sender status)


2. NOTIFICATION FLOW

   Post Service ──► Kafka (user-like) ──► Notification Service
                                                    │
                                                    ▼
                                          Save to MongoDB
                                                    │
                                                    ▼
                                          Kafka (notification.push)
                                                    │
                                                    ▼
                                          Realtime Gateway
                                                    │
                                                    ▼
                                          Push to User
```

---

# 📅 PHẦN 5: TIMELINE VÀ CHECKLIST

## 5.1 Thứ tự thực hiện đề xuất

```
Tuần 1-2: blur-common-lib Refactoring
├── [ ] Tạo các class mới trong common-lib
├── [ ] Update pom.xml với dependencies
├── [ ] Build và test common-lib
├── [ ] Update từng service (xóa duplicate classes)
└── [ ] Integration testing

Tuần 3: JWT to Cookie Migration  
├── [ ] Update IdentityService AuthController
├── [ ] Create CookieToHeaderFilter trong API Gateway
├── [ ] Update frontend API calls
├── [ ] Update WebSocket authentication
└── [ ] End-to-end testing

Tuần 4-5: Realtime Gateway + WebSocket Unification
├── [ ] Tạo realtime-gateway service
├── [ ] Implement STOMP handlers
├── [ ] Implement Kafka consumers/producers
├── [ ] Migrate WebRTC signaling
├── [ ] Update frontend WebSocket client
├── [ ] Testing voice/video calls
└── [ ] Deprecate old Socket.IO endpoint

Tuần 6: Kafka Integration Enhancement
├── [ ] Tạo thêm Kafka topics
├── [ ] Update notification-service consumers
├── [ ] Update chat-service với Kafka
├── [ ] End-to-end flow testing
└── [ ] Performance testing

Buffer: 1 tuần cho bugs và refinements
```

## 5.2 Testing Checklist

### Common-lib Tests

- [ ] All services can import và use common classes
- [ ] ErrorCode mappings correct
- [ ] Exception handling works
- [ ] JWT decoding works across services

### Cookie Authentication Tests

- [ ] Login sets HttpOnly cookie
- [ ] Logout clears cookie
- [ ] Token refresh works
- [ ] API Gateway extracts cookie correctly
- [ ] WebSocket authenticates via cookie

### WebSocket Tests

- [ ] Connection establishes with cookie auth
- [ ] Chat messages send/receive
- [ ] Typing indicators work
- [ ] Voice call signaling works
- [ ] Video call signaling works
- [ ] Notifications push correctly
- [ ] Reconnection works

### Kafka Tests

- [ ] Messages published correctly
- [ ] Consumers receive messages
- [ ] Message ordering preserved (per partition)
- [ ] Dead letter queue works

---

# 📝 TÓM TẮT

## Những gì sẽ thay đổi

| Component             | Thay đổi                                 | Complexity |
|-----------------------|------------------------------------------|------------|
| blur-common-lib       | Thêm 6+ classes                          | Medium     |
| All services          | Remove duplicate classes, update imports | Low        |
| IdentityService       | Cookie-based auth                        | Medium     |
| API Gateway           | Cookie extraction filter                 | Low        |
| Frontend              | Remove localStorage, use cookies         | Medium     |
| NEW: realtime-gateway | Unified WebSocket service                | High       |
| chat-service          | Remove SocketHandler, add Kafka          | Medium     |
| notification-service  | Update to use realtime-gateway           | Medium     |
| Frontend WebSocket    | New STOMP client                         | High       |

## Effort Estimate

| Phase                  | Days           | Risk            |
|------------------------|----------------|-----------------|
| Common-lib refactoring | 5-7 days       | Low             |
| JWT to Cookie          | 3-4 days       | Low             |
| Realtime Gateway       | 7-10 days      | High            |
| Kafka Enhancement      | 3-5 days       | Medium          |
| Testing & Bug fixes    | 5-7 days       | Medium          |
| **Total**              | **23-33 days** | **Medium-High** |

---

> **Lưu ý:** File này là hướng dẫn tổng quan. Trong quá trình implementation, có thể cần điều chỉnh dựa trên các edge
> cases phát hiện được.

---

*Được tạo bởi Senior Java Backend Developer Analysis*
*Ngày tạo: 15/01/2026*
