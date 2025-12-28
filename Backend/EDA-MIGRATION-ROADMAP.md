# 🚀 KẾ HOẠCH HỌC KAFKA & CHUYỂN ĐỔI EVENT-DRIVEN (Phiên bản Sinh viên)

> **Dành cho:** Sinh viên mới học Kafka, vừa làm vừa học
> **Thời gian:** 16-24 tuần (4-6 tháng)

---

## 📅 LỊCH CÔNG VIỆC THEO TUẦN (16-24 Tuần)

> **Ưu tiên:** Tối ưu codebase trước → JWT Cookie → Kafka cơ bản → Kafka nâng cao → AI Service

---

### 🔷 GIAI ĐOẠN 0: SETUP HYBRID .ENV (Tuần 0 - Chuẩn bị)

> **Mục tiêu:** Sử dụng Hybrid Approach - `.env.shared` cho config chung + `.env` riêng mỗi service

#### 📌 Tuần 0: Cấu hình Hybrid .env

- [ ] **Bước 1: Tạo file .env.shared trong thư mục Backend (config dùng chung)**
  ```bash
  cd Backend
  touch .env.shared
  ```
  
  ```env
  # Backend/.env.shared - CONFIG DÙNG CHUNG CHO TẤT CẢ SERVICES
  
  # ============================================
  # KAFKA CONFIGURATION (dùng chung)
  # ============================================
  KAFKA_BOOTSTRAP_SERVERS=localhost:9092
  KAFKA_GROUP_ID_PREFIX=blur
  
  # ============================================
  # REDIS CONFIGURATION (dùng chung)
  # ============================================
  REDIS_HOST=localhost
  REDIS_PORT=6379
  REDIS_PASSWORD=
  
  # ============================================
  # JWT CONFIGURATION (dùng chung)
  # ============================================
  JWT_SECRET_KEY=your-super-secret-key-at-least-256-bits-long-for-security
  JWT_ACCESS_TOKEN_EXPIRY=900000
  JWT_REFRESH_TOKEN_EXPIRY=604800000
  
  # ============================================
  # CORS CONFIGURATION (dùng chung)
  # ============================================
  CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
  
  # ============================================
  # AI SERVICE URL (dùng chung)
  # ============================================
  AI_SERVICE_URL=http://localhost:8086
  
  # ============================================
  # ENVIRONMENT
  # ============================================
  SPRING_PROFILES_ACTIVE=dev
  ```

- [ ] **Bước 2: Tạo .env riêng cho IdentityService**
  ```bash
  touch IdentityService/.env
  ```
  
  ```env
  # IdentityService/.env - CONFIG RIÊNG CHO IDENTITY SERVICE
  
  # Service Port
  SERVER_PORT=8080
  
  # MySQL Database (chỉ IdentityService dùng MySQL)
  MYSQL_HOST=localhost
  MYSQL_PORT=3306
  MYSQL_DATABASE=blur_identity
  MYSQL_USERNAME=root
  MYSQL_PASSWORD=your_mysql_password
  
  # Application name
  SPRING_APPLICATION_NAME=identity-service
  ```

- [ ] **Bước 3: Tạo .env riêng cho post-service**
  ```bash
  touch post-service/.env
  ```
  
  ```env
  # post-service/.env - CONFIG RIÊNG CHO POST SERVICE
  
  # Service Port
  SERVER_PORT=8081
  
  # MongoDB (post-service dùng MongoDB)
  MONGODB_URI=mongodb://localhost:27017/blur_posts
  
  # Kafka consumer group riêng
  KAFKA_CONSUMER_GROUP_ID=post-service-group
  
  # Application name
  SPRING_APPLICATION_NAME=post-service
  ```

- [ ] **Bước 4: Tạo .env riêng cho notification-service**
  ```bash
  touch notification-service/.env
  ```
  
  ```env
  # notification-service/.env
  
  SERVER_PORT=8083
  MONGODB_URI=mongodb://localhost:27017/blur_notifications
  KAFKA_CONSUMER_GROUP_ID=notification-service-group
  SPRING_APPLICATION_NAME=notification-service
  ```

- [ ] **Bước 5: Tạo .env riêng cho profile-service**
  ```bash
  touch profile-service/.env
  ```
  
  ```env
  # profile-service/.env
  
  SERVER_PORT=8082
  
  # Neo4j (chỉ profile-service dùng Neo4j)
  NEO4J_URI=bolt://localhost:7687
  NEO4J_USERNAME=neo4j
  NEO4J_PASSWORD=your_neo4j_password
  
  SPRING_APPLICATION_NAME=profile-service
  ```

- [ ] **Bước 6: Tạo .env cho các service còn lại**
  ```bash
  touch story-service/.env
  touch chat-service/.env
  touch api-gateway/.env
  ```

  ```env
  # story-service/.env
  SERVER_PORT=8084
  MONGODB_URI=mongodb://localhost:27017/blur_stories
  SPRING_APPLICATION_NAME=story-service
  
  # chat-service/.env
  SERVER_PORT=8085
  MONGODB_URI=mongodb://localhost:27017/blur_chat
  SPRING_APPLICATION_NAME=chat-service
  
  # api-gateway/.env
  SERVER_PORT=8888
  SPRING_APPLICATION_NAME=api-gateway
  ```

- [ ] **Bước 7: Cấu hình application.yml để load cả 2 file .env**

  **IdentityService/src/main/resources/application.yml:**
  ```yaml
  spring:
    config:
      import:
        - optional:file:../.env.shared[.properties]  # Load shared config trước
        - optional:file:./.env[.properties]          # Load service config sau (override)
    
    application:
      name: ${SPRING_APPLICATION_NAME}
    
    datasource:
      url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
      username: ${MYSQL_USERNAME}
      password: ${MYSQL_PASSWORD}
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    data:
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
    
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  
  server:
    port: ${SERVER_PORT}
  
  jwt:
    secret-key: ${JWT_SECRET_KEY}
    access-token-expiry: ${JWT_ACCESS_TOKEN_EXPIRY}
    refresh-token-expiry: ${JWT_REFRESH_TOKEN_EXPIRY}
  
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
  ```

  **post-service/src/main/resources/application.yml:**
  ```yaml
  spring:
    config:
      import:
        - optional:file:../.env.shared[.properties]
        - optional:file:./.env[.properties]
    
    application:
      name: ${SPRING_APPLICATION_NAME}
    
    data:
      mongodb:
        uri: ${MONGODB_URI}
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
    
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      consumer:
        group-id: ${KAFKA_CONSUMER_GROUP_ID}
  
  server:
    port: ${SERVER_PORT}
  ```

  **notification-service/src/main/resources/application.yml:**
  ```yaml
  spring:
    config:
      import:
        - optional:file:../.env.shared[.properties]
        - optional:file:./.env[.properties]
    
    application:
      name: ${SPRING_APPLICATION_NAME}
    
    data:
      mongodb:
        uri: ${MONGODB_URI}
    
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      consumer:
        group-id: ${KAFKA_CONSUMER_GROUP_ID}
  
  server:
    port: ${SERVER_PORT}
  ```

  **profile-service/src/main/resources/application.yml:**
  ```yaml
  spring:
    config:
      import:
        - optional:file:../.env.shared[.properties]
        - optional:file:./.env[.properties]
    
    application:
      name: ${SPRING_APPLICATION_NAME}
    
    neo4j:
      uri: ${NEO4J_URI}
      authentication:
        username: ${NEO4J_USERNAME}
        password: ${NEO4J_PASSWORD}
    
    data:
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
  
  server:
    port: ${SERVER_PORT}
  ```

- [ ] **Bước 8: Update .gitignore**
  ```bash
  # Thêm vào .gitignore
  echo ".env" >> .gitignore
  echo ".env.shared" >> .gitignore
  echo "*/.env" >> .gitignore
  ```

- [ ] **Bước 10: Update docker-compose.yml**
  ```yaml
  # Backend/docker-compose.yml
  version: '3.8'
  
  services:
    mysql:
      image: mysql:8.0
      container_name: blur-mysql
      environment:
        MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:-root}
        MYSQL_DATABASE: blur_identity
      ports:
        - "3306:3306"
      volumes:
        - mysql_data:/var/lib/mysql
    
    mongodb:
      image: mongo:7.0
      container_name: blur-mongodb
      ports:
        - "27017:27017"
      volumes:
        - mongodb_data:/data/db
    
    neo4j:
      image: neo4j:5
      container_name: blur-neo4j
      environment:
        NEO4J_AUTH: neo4j/${NEO4J_PASSWORD:-password}
      ports:
        - "7474:7474"
        - "7687:7687"
      volumes:
        - neo4j_data:/data
    
    redis:
      image: redis:7-alpine
      container_name: blur-redis
      ports:
        - "6379:6379"
    
    zookeeper:
      image: confluentinc/cp-zookeeper:7.7.1
      container_name: blur-zookeeper
      environment:
        ZOOKEEPER_CLIENT_PORT: 2181
    
    kafka:
      image: confluentinc/cp-kafka:7.7.1
      container_name: blur-kafka
      depends_on:
        - zookeeper
      ports:
        - "9092:9092"
      environment:
        KAFKA_BROKER_ID: 1
        KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
        KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9093,PLAINTEXT_HOST://localhost:9092
        KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
        KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    
    kafka-ui:
      image: provectuslabs/kafka-ui:latest
      container_name: blur-kafka-ui
      ports:
        - "8080:8080"
      environment:
        KAFKA_CLUSTERS_0_NAME: blur-local
        KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9093
  
  volumes:
    mysql_data:
    mongodb_data:
    neo4j_data:
  ```

- [ ] **Bước 11: Test chạy từng service**
  ```bash
  # 1. Start infrastructure
  docker-compose up -d mysql mongodb neo4j redis kafka zookeeper
  
  # 2. Wait for services to be ready
  sleep 10
  
  # 3. Test IdentityService
  cd IdentityService
  mvn spring-boot:run
  # Check log - không có lỗi config → OK
  
  # 4. Test post-service (terminal mới)
  cd post-service
  mvn spring-boot:run
  
  # 5. Tiếp tục với các service khác
  ```

**✅ Milestone Tuần 0:** Hybrid .env setup hoàn chỉnh, tất cả services chạy đúng

---

### Cấu trúc thư mục sau khi setup (Hybrid)

```
Backend/
├── .env.shared             # 👈 CONFIG CHUNG (Kafka, Redis, JWT, CORS)
├── .env.shared.example     # 👈 Template cho .env.shared
├── .gitignore
├── docker-compose.yml
│
├── IdentityService/
│   ├── .env                # 👈 CONFIG RIÊNG (MySQL, Port)
│   ├── .env.example
│   └── src/main/resources/
│       └── application.yml # 👈 Load cả 2 file
│
├── post-service/
│   ├── .env                # 👈 CONFIG RIÊNG (MongoDB, Port)
│   ├── .env.example
│   └── src/main/resources/
│       └── application.yml
│
├── profile-service/
│   ├── .env                # 👈 CONFIG RIÊNG (Neo4j, Port)
│   └── ...
│
└── notification-service/
    ├── .env                # 👈 CONFIG RIÊNG (MongoDB, Port)
    └── ...
```

---

### Lợi ích của Hybrid Approach

| Aspect | Lợi ích |
|--------|---------|
| **Không duplicate** | Kafka, Redis, JWT config chỉ viết 1 lần trong `.env.shared` |
| **Độc lập** | Mỗi service có config riêng (DB, Port) |
| **Security** | Service chỉ biết credentials của nó |
| **Dễ tách repo** | Sau này có thể copy service ra repo riêng dễ dàng |
| **Clear ownership** | Biết rõ config nào thuộc service nào |

---

### Quick Reference: File nào chứa config gì?

| Config | .env.shared | Service/.env |
|--------|-------------|--------------|
| Kafka | ✅ | |
| Redis | ✅ | |
| JWT Secret | ✅ | |
| CORS | ✅ | |
| MySQL | | IdentityService |
| MongoDB | | post, notification, story, chat |
| Neo4j | | profile-service |
| Server Port | | Mỗi service |

---

### 🔷 GIAI ĐOẠN 1: TỐI ƯU CODEBASE (Tuần 1-4)

#### 📌 Tuần 1: Setup Common Library

- [ ] **T2-T3: Tạo thư mục và pom.xml**
  ```bash
  # Tạo thư mục
  mkdir -p Backend/blur-common-lib/src/main/java/com/blur/common
  cd Backend/blur-common-lib
  ```
  
  ```xml
  <!-- blur-common-lib/pom.xml -->
  <?xml version="1.0" encoding="UTF-8"?>
  <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.blur</groupId>
      <artifactId>blur-common-lib</artifactId>
      <version>1.0.0</version>
      <packaging>jar</packaging>
      
      <parent>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-parent</artifactId>
          <version>3.2.0</version>
      </parent>
      
      <dependencies>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
      </dependencies>
  </project>
  ```

- [ ] **T4-T5: Tạo ApiResponse.java**
  ```java
  // blur-common-lib/src/main/java/com/blur/common/dto/ApiResponse.java
  package com.blur.common.dto;
  
  import lombok.*;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class ApiResponse<T> {
      @Builder.Default
      private int code = 200;
      private String message;
      private T result;
      
      public static <T> ApiResponse<T> success(T data) {
          return ApiResponse.<T>builder()
              .code(200).message("Success").result(data).build();
      }
      
      public static <T> ApiResponse<T> error(int code, String msg) {
          return ApiResponse.<T>builder()
              .code(code).message(msg).build();
      }
  }
  ```

- [ ] **T6-CN: Test build**
  ```bash
  cd Backend/blur-common-lib
  mvn clean install
  # Output: BUILD SUCCESS
  ```

---

#### 📌 Tuần 2: Exception Handling

- [ ] **T2-T3: Tạo BlurException**
  ```java
  // blur-common-lib/src/main/java/com/blur/common/exception/BlurException.java
  package com.blur.common.exception;
  
  import lombok.Getter;
  
  @Getter
  public class BlurException extends RuntimeException {
      private final int errorCode;
      
      public BlurException(int errorCode, String message) {
          super(message);
          this.errorCode = errorCode;
      }
  }
  ```

- [ ] **T2-T3: Tạo ResourceNotFoundException**
  ```java
  // blur-common-lib/src/main/java/com/blur/common/exception/ResourceNotFoundException.java
  package com.blur.common.exception;
  
  public class ResourceNotFoundException extends BlurException {
      public ResourceNotFoundException(String resource, String id) {
          super(404, resource + " not found with id: " + id);
      }
  }
  ```

- [ ] **T4-T5: Tạo GlobalExceptionHandler**
  ```java
  // blur-common-lib/src/main/java/com/blur/common/exception/GlobalExceptionHandler.java
  package com.blur.common.exception;
  
  import com.blur.common.dto.ApiResponse;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.*;
  
  @RestControllerAdvice
  public class GlobalExceptionHandler {
      
      @ExceptionHandler(BlurException.class)
      public ResponseEntity<ApiResponse<?>> handleBlurException(BlurException e) {
          return ResponseEntity.status(e.getErrorCode())
              .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
      }
      
      @ExceptionHandler(Exception.class)
      public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
          return ResponseEntity.status(500)
              .body(ApiResponse.error(500, "Internal server error"));
      }
  }
  ```

- [ ] **T6-CN: Build lại và test**
  ```bash
  mvn clean install
  ```

---

#### 📌 Tuần 3: Integrate vào 2 service đầu tiên

- [ ] **T2-T3: Thêm dependency vào post-service**
  ```xml
  <!-- post-service/pom.xml - thêm vào <dependencies> -->
  <dependency>
      <groupId>com.blur</groupId>
      <artifactId>blur-common-lib</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```

- [ ] **T4-T5: Thêm dependency vào notification-service**
  ```xml
  <!-- notification-service/pom.xml - thêm vào <dependencies> -->
  <dependency>
      <groupId>com.blur</groupId>
      <artifactId>blur-common-lib</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```

- [ ] **T6-CN: Refactor code sử dụng class mới**
  ```java
  // Trước (code cũ)
  @GetMapping("/posts/{id}")
  public ResponseEntity<?> getPost(@PathVariable String id) {
      Post post = postService.findById(id);
      if (post == null) {
          return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(post);
  }
  
  // Sau (dùng common-lib)
  import com.blur.common.dto.ApiResponse;
  import com.blur.common.exception.ResourceNotFoundException;
  
  @GetMapping("/posts/{id}")
  public ApiResponse<Post> getPost(@PathVariable String id) {
      Post post = postService.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Post", id));
      return ApiResponse.success(post);
  }
  ```

- [ ] **Test API để đảm bảo hoạt động**
  ```bash
  # Test endpoint
  curl http://localhost:8081/posts/123
  # Expected: {"code":404,"message":"Post not found with id: 123"}
  ```

---

#### 📌 Tuần 4: Integrate các service còn lại

- [ ] **T2: Thêm vào IdentityService**
- [ ] **T3: Thêm vào profile-service**
- [ ] **T4: Thêm vào story-service**
- [ ] **T5: Thêm vào chat-service**
- [ ] **T6-CN: Test toàn bộ service**
  ```bash
  # Build tất cả
  cd Backend/IdentityService && mvn clean compile
  cd Backend/profile-service && mvn clean compile
  cd Backend/story-service && mvn clean compile
  cd Backend/chat-service && mvn clean compile
  # Tất cả phải BUILD SUCCESS
  ```

**✅ Milestone Tuần 4:** Tất cả 7 service đã dùng `blur-common-lib`

---

### 🔷 GIAI ĐOẠN 2: JWT COOKIE (Tuần 5-7)

#### 📌 Tuần 5: Backend - CookieUtils & AuthController

- [ ] **T2-T3: Tạo CookieUtils.java**
  ```java
  // blur-common-lib/src/main/java/com/blur/common/security/CookieUtils.java
  package com.blur.common.security;
  
  import jakarta.servlet.http.Cookie;
  import jakarta.servlet.http.HttpServletRequest;
  import org.springframework.http.ResponseCookie;
  import org.springframework.stereotype.Component;
  import java.time.Duration;
  import java.util.*;
  
  @Component
  public class CookieUtils {
      public static final String ACCESS_TOKEN = "access_token";
      public static final String REFRESH_TOKEN = "refresh_token";
      
      public ResponseCookie createAccessTokenCookie(String token, long seconds) {
          return ResponseCookie.from(ACCESS_TOKEN, token)
              .httpOnly(true).secure(true).sameSite("Strict")
              .path("/").maxAge(Duration.ofSeconds(seconds)).build();
      }
      
      public ResponseCookie createRefreshTokenCookie(String token, long seconds) {
          return ResponseCookie.from(REFRESH_TOKEN, token)
              .httpOnly(true).secure(true).sameSite("Strict")
              .path("/api/auth/refresh").maxAge(Duration.ofSeconds(seconds)).build();
      }
      
      public ResponseCookie deleteAccessTokenCookie() {
          return ResponseCookie.from(ACCESS_TOKEN, "")
              .httpOnly(true).secure(true).sameSite("Strict")
              .path("/").maxAge(0).build();
      }
      
      public Optional<String> getTokenFromCookie(HttpServletRequest req, String name) {
          if (req.getCookies() == null) return Optional.empty();
          return Arrays.stream(req.getCookies())
              .filter(c -> name.equals(c.getName()))
              .map(Cookie::getValue).findFirst();
      }
  }
  ```

- [ ] **T4-T5: Sửa AuthController - login trả cookie**
  ```java
  // IdentityService - AuthController.java
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<UserInfo>> login(
          @RequestBody LoginRequest request,
          HttpServletResponse response) {
      
      AuthResult result = authService.login(request);
      String accessToken = jwtProvider.generateAccessToken(result.getUserId());
      String refreshToken = jwtProvider.generateRefreshToken(result.getUserId());
      
      // Set cookies
      response.addHeader("Set-Cookie", 
          cookieUtils.createAccessTokenCookie(accessToken, 900).toString()); // 15 min
      response.addHeader("Set-Cookie", 
          cookieUtils.createRefreshTokenCookie(refreshToken, 604800).toString()); // 7 days
      
      return ResponseEntity.ok(ApiResponse.success(result.getUserInfo()));
  }
  ```

- [ ] **T6-CN: Sửa refresh và logout endpoint**
  ```java
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<Void>> refresh(
          HttpServletRequest request, HttpServletResponse response) {
      String refreshToken = cookieUtils.getTokenFromCookie(request, "refresh_token")
          .orElseThrow(() -> new BlurException(401, "No refresh token"));
      // ... validate and create new tokens
      return ResponseEntity.ok(ApiResponse.success(null));
  }
  
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
      response.addHeader("Set-Cookie", cookieUtils.deleteAccessTokenCookie().toString());
      return ResponseEntity.ok(ApiResponse.success("Logged out"));
  }
  ```

---

#### 📌 Tuần 6: Backend - JwtFilter & Security Config

- [ ] **T2-T3: Sửa JwtAuthenticationFilter**
  ```java
  // Đọc token từ Cookie thay vì Header
  @Override
  protected void doFilterInternal(HttpServletRequest request, 
          HttpServletResponse response, FilterChain chain) {
      
      Optional<String> tokenOpt = cookieUtils.getTokenFromCookie(request, "access_token");
      
      if (tokenOpt.isPresent() && jwtProvider.validateToken(tokenOpt.get())) {
          String userId = jwtProvider.getUserIdFromToken(tokenOpt.get());
          var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
          SecurityContextHolder.getContext().setAuthentication(auth);
      }
      
      chain.doFilter(request, response);
  }
  ```

- [ ] **T4-T5: Update SecurityConfig - CORS**
  ```java
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
      config.setAllowedHeaders(List.of("*"));
      config.setAllowCredentials(true); // QUAN TRỌNG cho Cookie!
      
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", config);
      return source;
  }
  ```

- [ ] **T6-CN: Test bằng Postman**
  ```
  1. POST /api/auth/login → Check Response Headers có Set-Cookie
  2. GET /api/profile/me → Check Cookie được gửi đi
  ```

---

#### 📌 Tuần 7: Frontend Integration

- [ ] **T2-T3: Sửa Axios config**
  ```javascript
  // src/api/axios.js
  import axios from 'axios';
  
  const api = axios.create({
      baseURL: 'http://localhost:8080',
      withCredentials: true,  // QUAN TRỌNG!
  });
  
  export default api;
  ```

- [ ] **T4-T5: Implement auto refresh interceptor**
  ```javascript
  api.interceptors.response.use(
      response => response,
      async error => {
          if (error.response?.status === 401 && !error.config._retry) {
              error.config._retry = true;
              try {
                  await axios.post('/api/auth/refresh', {}, { withCredentials: true });
                  return api(error.config);
              } catch {
                  window.location.href = '/login';
              }
          }
          return Promise.reject(error);
      }
  );
  ```

- [ ] **T6-CN: Test full flow**
  ```
  1. Login → Check không có token trong localStorage
  2. Gọi API → Check request có Cookie
  3. Logout → Check Cookie bị xóa
  ```

**✅ Milestone Tuần 7:** JWT lưu trong Cookie, FE + BE hoạt động

---

### 🔷 GIAI ĐOẠN 3: KAFKA CƠ BẢN (Tuần 8-11)

#### 📌 Tuần 8: Tìm hiểu & Setup

- [ ] **T2-T3: Xem video học Kafka**
  - Video 1: "What is Apache Kafka?" (10-15 phút)
  - Video 2: "Kafka Producer Consumer" (15-20 phút)

- [ ] **T4-T5: Chạy Kafka với Docker**
  ```bash
  cd Backend
  docker-compose up -d kafka zookeeper
  
  # Verify
  docker ps
  # Phải thấy kafka và zookeeper đang chạy
  ```

- [ ] **T6-CN: Cài Kafka UI**
  ```yaml
  # Thêm vào docker-compose.yml
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9093
  ```
  ```bash
  docker-compose up -d kafka-ui
  # Mở http://localhost:8080
  ```

---

#### 📌 Tuần 9: Producer đơn giản

- [ ] **T2-T3: Thêm spring-kafka dependency**
  ```xml
  <!-- post-service/pom.xml -->
  <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka</artifactId>
  </dependency>
  ```

- [ ] **T4-T5: Tạo SimpleProducer**
  ```java
  // post-service/src/main/java/.../kafka/SimpleProducer.java
  package com.blur.postservice.kafka;
  
  import lombok.RequiredArgsConstructor;
  import org.springframework.kafka.core.KafkaTemplate;
  import org.springframework.stereotype.Service;
  
  @Service
  @RequiredArgsConstructor
  public class SimpleProducer {
      private final KafkaTemplate<String, String> kafkaTemplate;
      
      public void send(String message) {
          kafkaTemplate.send("test-topic", message);
          System.out.println("✅ Sent: " + message);
      }
  }
  ```

- [ ] **T6-CN: Tạo test endpoint**
  ```java
  @RestController
  @RequestMapping("/test")
  @RequiredArgsConstructor
  public class TestKafkaController {
      private final SimpleProducer producer;
      
      @PostMapping("/send")
      public String send(@RequestBody String message) {
          producer.send(message);
          return "Sent!";
      }
  }
  ```
  ```bash
  # Test
  curl -X POST http://localhost:8081/test/send -d "Hello Kafka"
  # Check Kafka UI → Topics → test-topic → Messages
  ```

---

#### 📌 Tuần 10: Consumer đơn giản

- [ ] **T2-T3: Tạo SimpleConsumer**
  ```java
  // notification-service/src/main/java/.../kafka/SimpleConsumer.java
  package com.blur.notificationservice.kafka;
  
  import org.springframework.kafka.annotation.KafkaListener;
  import org.springframework.stereotype.Service;
  
  @Service
  public class SimpleConsumer {
      
      @KafkaListener(topics = "test-topic", groupId = "notification-group")
      public void listen(String message) {
          System.out.println("📩 Received: " + message);
      }
  }
  ```

- [ ] **T4-T5: Cấu hình application.yml**
  ```yaml
  # notification-service/src/main/resources/application.yml
  spring:
    kafka:
      bootstrap-servers: localhost:9092
      consumer:
        group-id: notification-group
        auto-offset-reset: earliest
  ```

- [ ] **T6-CN: Test gửi & nhận**
  ```bash
  # Terminal 1: Chạy notification-service
  # Terminal 2: Gửi message
  curl -X POST http://localhost:8081/test/send -d "Test message"
  # Check Terminal 1 có log "📩 Received: Test message"
  ```

---

#### 📌 Tuần 11: Đọc hiểu notification-service

- [ ] **T2-T3: Đọc EventListener.java**
  - File: `notification-service/src/main/java/.../kafka/consumer/EventListener.java`
  - Hiểu: Topics đang listen, cách route message

- [ ] **T4-T5: Đọc các handler**
  - Folder: `notification-service/src/main/java/.../kafka/handler/`
  - Hiểu: Mỗi handler xử lý 1 loại event

- [ ] **T6-CN: Trigger event từ FE**
  - Like 1 post → Xem log notification-service
  - Follow 1 user → Xem notification xuất hiện

**✅ Milestone Tuần 11:** Hiểu Kafka cơ bản, biết gửi/nhận message

---

### 🔷 GIAI ĐOẠN 4: COMMENT MODERATION FLOW (Tuần 12-16)

#### 📌 Tuần 12: Event Classes & Producer

- [ ] **T2-T3: Tạo CommentModerationEvent**
  ```java
  // post-service/src/main/java/.../kafka/event/CommentModerationEvent.java
  @Data @Builder @AllArgsConstructor @NoArgsConstructor
  public class CommentModerationEvent {
      private String eventId;
      private String commentId;
      private String content;
      private String authorId;
      private String postId;
      private Long timestamp;
  }
  ```

- [ ] **T4-T5: Tạo CommentEventProducer**
  ```java
  @Slf4j @Component @RequiredArgsConstructor
  public class CommentEventProducer {
      private final KafkaTemplate<String, String> kafka;
      private final ObjectMapper mapper;
      
      public void sendForModeration(Comment comment) {
          try {
              var event = CommentModerationEvent.builder()
                  .eventId(UUID.randomUUID().toString())
                  .commentId(comment.getId())
                  .content(comment.getContent())
                  .authorId(comment.getAuthorId())
                  .postId(comment.getPostId())
                  .timestamp(System.currentTimeMillis())
                  .build();
              kafka.send("comment-moderation-request", comment.getId(), 
                  mapper.writeValueAsString(event));
              log.info("✅ Sent comment {} for moderation", comment.getId());
          } catch (Exception e) {
              log.error("❌ Error: {}", e.getMessage());
          }
      }
  }
  ```

- [ ] **T6-CN: Test gửi event**
  ```bash
  # Tạo comment → Check Kafka UI có message trong topic
  ```

---

#### 📌 Tuần 13: Python AI Service Setup

- [ ] **T2-T3: Tạo FastAPI project**
  ```bash
  mkdir -p Backend/ai-service-python
  cd Backend/ai-service-python
  ```
  ```python
  # requirements.txt
  fastapi==0.104.1
  uvicorn==0.24.0
  kafka-python==2.0.2
  transformers==4.35.2
  torch==2.1.1
  ```

- [ ] **T4-T5: Load model và test**
  ```python
  # model.py
  from transformers import AutoTokenizer, AutoModelForSequenceClassification
  import torch
  
  class ToxicClassifier:
      def __init__(self, model_path="./models/phobert-toxic"):
          self.tokenizer = AutoTokenizer.from_pretrained(model_path)
          self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
          self.model.eval()
      
      def predict(self, text):
          inputs = self.tokenizer(text, return_tensors="pt", truncation=True)
          with torch.no_grad():
              outputs = self.model(**inputs)
          is_toxic = torch.sigmoid(outputs.logits)[0][0].item() > 0.5
          return {"is_toxic": is_toxic}
  ```

- [ ] **T6-CN: Thêm Kafka consumer**
  ```python
  # consumer.py
  from kafka import KafkaConsumer
  import json
  
  consumer = KafkaConsumer(
      'comment-moderation-request',
      bootstrap_servers='localhost:9092',
      value_deserializer=lambda m: json.loads(m.decode('utf-8'))
  )
  
  for message in consumer:
      print(f"Received: {message.value}")
  ```

---

#### 📌 Tuần 14: AI Service Complete

- [ ] **T2-T3: Xử lý message**
- [ ] **T4-T5: Gửi kết quả về topic mới**
  ```python
  from kafka import KafkaProducer
  
  producer = KafkaProducer(
      bootstrap_servers='localhost:9092',
      value_serializer=lambda m: json.dumps(m).encode('utf-8')
  )
  
  # Sau khi predict
  result = {
      "commentId": event["commentId"],
      "isToxic": prediction["is_toxic"],
      "action": "REJECT" if prediction["is_toxic"] else "APPROVE"
  }
  producer.send('comment-moderation-result', result)
  ```

- [ ] **T6-CN: Test full flow**

---

#### 📌 Tuần 15: Result Consumer & Status Update

- [ ] **T2-T3: Tạo ModerationResultConsumer**
  ```java
  @Component @RequiredArgsConstructor @Slf4j
  public class ModerationResultConsumer {
      private final CommentRepository commentRepo;
      private final ObjectMapper mapper;
      
      @KafkaListener(topics = "comment-moderation-result", groupId = "post-moderation")
      public void handle(String message) {
          var event = mapper.readValue(message, ModerationResultEvent.class);
          var comment = commentRepo.findById(event.getCommentId()).orElse(null);
          if (comment != null) {
              comment.setStatus(event.getIsToxic() ? CommentStatus.REJECTED : CommentStatus.APPROVED);
              commentRepo.save(comment);
              log.info("Updated comment {} to {}", event.getCommentId(), comment.getStatus());
          }
      }
  }
  ```

- [ ] **T4-T5: Test APPROVED flow**
- [ ] **T6-CN: Test REJECTED flow**

---

#### 📌 Tuần 16: Error Handling & Testing

- [ ] **T2-T3: Thêm try-catch và logging**
- [ ] **T4-T5: Test AI down, Kafka down**
- [ ] **T6-CN: Clean up code**

**✅ Milestone Tuần 16:** Comment moderation flow hoàn chỉnh

---

### 🔷 GIAI ĐOẠN 5: NÂNG CAO (Tuần 17-24) - Optional

#### 📌 Tuần 17-18: Idempotent Consumer

- [ ] Tìm hiểu idempotency là gì
- [ ] Tạo `IdempotentConsumerService` với Redis
- [ ] Test duplicate message không xử lý trùng

#### 📌 Tuần 19-20: Retry & Dead Letter Queue

- [ ] Config retry 3 lần
- [ ] Setup DLQ topic
- [ ] Alert khi có message vào DLQ

#### 📌 Tuần 21-22: Áp dụng cho service khác

- [ ] Story service - thêm Kafka event
- [ ] Profile service - thêm Kafka event
- [ ] Test notification đa dạng

#### 📌 Tuần 23-24: Monitoring & Documentation

- [ ] Setup Kafka monitoring dashboard
- [ ] Viết README cho team
- [ ] Code review và refactor

**✅ Milestone Tuần 24:** Production-ready system

---

## 📊 TỔNG QUAN GIAI ĐOẠN

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ROADMAP 24 TUẦN (6 THÁNG)                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Tuần 1-4      │░░░░░░░░░░│ TỐI ƯU CODEBASE (Common Library)            │
│  ──────────────┼──────────┼───────────────────────────────────────────  │
│  Tuần 5-7      │░░░░░░│    JWT COOKIE (Security)                        │
│  ──────────────┼──────────┼───────────────────────────────────────────  │
│  Tuần 8-11     │░░░░░░░░│  KAFKA CƠ BẢN (Học & Thực hành)               │
│  ──────────────┼──────────┼───────────────────────────────────────────  │
│  Tuần 12-16    │░░░░░░░░░░│ COMMENT MODERATION (AI Integration)         │
│  ──────────────┼──────────┼───────────────────────────────────────────  │
│  Tuần 17-24    │░░░░░░░░░░░░░░░░│ NÂNG CAO (Optional)                   │
│                                                                          │
│  💡 Có thể dừng ở tuần 16 nếu đủ cho đồ án!                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ⏱️ ESTIMATE THỜI GIAN

| Level | Thời gian/ngày | Hoàn thành trong |
|-------|----------------|------------------|
| **Full-time** (nghỉ học) | 6-8h | 16 tuần |
| **Part-time** (đi học) | 2-3h | 20-24 tuần |
| **Weekend only** | 8-10h/tuần | 24-30 tuần |

---

## 📚 KAFKA LÀ GÌ? (Giải thích đơn giản)

```
🍕 Ví dụ thực tế: QUÁN PIZZA

┌──────────────────────────────────────────────────────────────────┐
│                                                                   │
│   KHÁCH HÀNG (Producer)          QUÁN PIZZA (Kafka)              │
│   ┌─────────┐                   ┌─────────────────┐              │
│   │ Đặt đơn │  ─────────────▶   │ Nhận đơn đặt    │              │
│   │ hàng    │                   │ (Topic: orders) │              │
│   └─────────┘                   └────────┬────────┘              │
│                                          │                        │
│                                          ▼                        │
│   ┌─────────┐                   ┌─────────────────┐              │
│   │ Làm     │  ◀─────────────   │ Giao việc cho   │              │
│   │ pizza   │                   │ đầu bếp         │              │
│   └─────────┘ (Consumer)        └─────────────────┘              │
│                                                                   │
│   🔑 ĐIỂM QUAN TRỌNG:                                            │
│   - Khách đặt xong → đi làm việc khác (không chờ)                │
│   - Đầu bếp nhận đơn khi rảnh → làm pizza                        │
│   - Nếu đầu bếp bận → đơn chờ trong hàng đợi                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Thuật ngữ cơ bản

| Thuật ngữ | Giải thích | Ví dụ trong hệ thống Blur |
|-----------|-----------|---------------------------|
| **Producer** | Service gửi message | Post Service tạo comment |
| **Consumer** | Service nhận và xử lý message | Notification Service gửi thông báo |
| **Topic** | "Kênh" chứa message theo chủ đề | `comment-events`, `like-events` |
| **Message** | Dữ liệu được gửi đi | `{userId: "123", action: "like", postId: "456"}` |
| **Broker** | Server Kafka lưu trữ message | Chạy trong Docker của bạn |

---

## 🗺️ LỘ TRÌNH HỌC (Vừa làm vừa học)

### Tuần 1-2: Hiểu Kafka cơ bản

**Mục tiêu:** Chạy được Kafka và gửi/nhận message đơn giản

```
📖 HỌC:
├── Kafka là gì, tại sao dùng (2-3 video YouTube)
├── Cài đặt Kafka với Docker (đã có trong docker-compose.yml)
└── Producer/Consumer là gì

💻 LÀM:
├── Bước 1: docker-compose up (chạy Kafka)
├── Bước 2: Tạo producer đơn giản trong post-service
├── Bước 3: Tạo consumer đơn giản trong notification-service
└── Bước 4: Gửi thử 1 message và xem console log
```

**Code mẫu đơn giản nhất:**

```java
// Producer - Gửi message (trong post-service)
@Service
public class SimpleProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void sendMessage(String message) {
        kafkaTemplate.send("my-first-topic", message);
        System.out.println("✅ Đã gửi: " + message);
    }
}

// Consumer - Nhận message (trong notification-service)  
@Service
public class SimpleConsumer {
    @KafkaListener(topics = "my-first-topic", groupId = "my-group")
    public void listen(String message) {
        System.out.println("📩 Nhận được: " + message);
    }
}
```

---

### Tuần 3-4: Áp dụng vào Notification

**Mục tiêu:** Hiểu notification-service hiện tại đang dùng Kafka như thế nào

```
📖 HỌC:
├── Xem code EventListener.java trong notification-service
├── Hiểu Topic: user-follow-events, user-like-events, user-comment-events
└── Tìm hiểu cách serialize JSON

💻 LÀM:
├── Bước 1: Đọc hiểu code notification-service/kafka/
├── Bước 2: Chạy thử và xem log khi có event
├── Bước 3: Thử sửa nhỏ (thêm log, đổi format)
└── Bước 4: Viết thêm 1 handler mới (copy từ handler có sẵn)
```

**Code hiện có trong hệ thống:**

```java
// notification-service đã có sẵn - Xem và học từ đây
@KafkaListener(
    topics = {"user-follow-events", "user-like-events", "user-comment-events"},
    groupId = "notification-service"
)
public void listen(ConsumerRecord<String, String> record, @Header(...) String topic) {
    // Xử lý message theo từng topic
}
```

---

### Tuần 5-6: Thêm Topic mới cho Comment

**Mục tiêu:** Tạo flow gửi comment sang AI kiểm tra toxic

```
📖 HỌC:
├── Hiểu flow: Comment → Kafka → AI Service → Kafka → Update status
└── Cách đặt tên topic (đơn giản: entity-action, VD: comment-created)

💻 LÀM:
├── Bước 1: Tạo topic mới: comment-moderation-request
├── Bước 2: Khi tạo comment → gửi event
├── Bước 3: AI service nhận event → xử lý → gửi kết quả
└── Bước 4: Post service nhận kết quả → update comment status
```

**Flow đơn giản:**

```
┌──────────────────────────────────────────────────────────────────┐
│                    LUỒNG XỬ LÝ COMMENT TOXIC                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. User viết comment                                            │
│     ┌─────────────┐                                              │
│     │ "Bài viết   │                                              │
│     │  hay lắm!"  │                                              │
│     └──────┬──────┘                                              │
│            │                                                      │
│            ▼                                                      │
│  2. Post Service lưu comment (status = PENDING)                  │
│     và gửi event tới Kafka                                       │
│            │                                                      │
│            ▼                                                      │
│     ┌─────────────────────────────────────┐                      │
│     │ Topic: comment-moderation-request   │                      │
│     │ Message: {commentId, content, ...}  │                      │
│     └──────────────────┬──────────────────┘                      │
│                        │                                          │
│                        ▼                                          │
│  3. AI Service nhận message, check toxic                         │
│     ┌─────────────────────────────────────┐                      │
│     │ PhoBERT: "Bài viết hay lắm!"        │                      │
│     │ Kết quả: NOT TOXIC ✅               │                      │
│     └──────────────────┬──────────────────┘                      │
│                        │                                          │
│                        ▼                                          │
│     ┌─────────────────────────────────────┐                      │
│     │ Topic: comment-moderation-result    │                      │
│     │ Message: {commentId, isToxic: false}│                      │
│     └──────────────────┬──────────────────┘                      │
│                        │                                          │
│                        ▼                                          │
│  4. Post Service nhận kết quả → update status = APPROVED         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

### Tuần 7-8: Xử lý lỗi cơ bản

**Mục tiêu:** Học cách xử lý khi có lỗi

```
📖 HỌC:
├── Message bị lỗi thì sao? (retry)
├── Retry 3 lần vẫn lỗi? (dead letter queue - DLQ)
└── Làm sao tránh xử lý trùng? (idempotency)

💻 LÀM:
├── Bước 1: Config retry: maxAttempts = 3
├── Bước 2: Tạo DLQ topic cho message lỗi
├── Bước 3: Log message lỗi để debug
└── Bước 4: Dùng Redis check message đã xử lý chưa
```

**Code xử lý lỗi đơn giản:**

```java
@Service
public class SafeConsumer {
    @Autowired
    private RedisTemplate<String, String> redis;
    
    @KafkaListener(topics = "comment-moderation-result")
    public void handleResult(String message) {
        // 1. Parse message
        var event = parseJson(message);
        String eventId = event.getEventId();
        
        // 2. Check đã xử lý chưa (tránh xử lý trùng)
        if (redis.hasKey("processed:" + eventId)) {
            System.out.println("⚠️ Đã xử lý rồi, bỏ qua!");
            return;
        }
        
        try {
            // 3. Xử lý logic
            updateCommentStatus(event);
            
            // 4. Đánh dấu đã xử lý
            redis.opsForValue().set("processed:" + eventId, "1", Duration.ofDays(1));
            
        } catch (Exception e) {
            // 5. Nếu lỗi → throw để Kafka retry
            System.out.println("❌ Lỗi: " + e.getMessage());
            throw e;
        }
    }
}
```

---

## 📂 CẤU TRÚC THƯ MỤC ĐƠN GIẢN

```
post-service/
├── src/main/java/com/blur/postservice/
│   ├── controller/         # REST API
│   ├── service/            # Business logic
│   ├── repository/         # Database access
│   ├── entity/             # Model classes
│   ├── dto/                # Request/Response objects
│   │
│   └── kafka/              # 👈 THÊM MỚI CHO KAFKA
│       ├── producer/       # Gửi message
│       │   └── CommentEventProducer.java
│       ├── consumer/       # Nhận message
│       │   └── ModerationResultConsumer.java
│       └── event/          # Định nghĩa message format
│           └── CommentEvent.java
```

---

## � VÍ DỤ CHI TIẾT: POST-SERVICE VỚI KAFKA

> **Mục tiêu:** Khi user tạo comment → gửi event để AI kiểm tra toxic → nhận kết quả → update status

### Bước 1: Thêm dependency vào pom.xml

```xml
<!-- Thêm vào file pom.xml của post-service -->
<dependencies>
    <!-- ... các dependency khác ... -->
    
    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

---

### Bước 2: Cấu hình Kafka trong application.yml

```yaml
# post-service/src/main/resources/application.yml
spring:
  application:
    name: post-service
  
  kafka:
    bootstrap-servers: localhost:9092
    
    # Config cho Producer (gửi message)
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      # Đảm bảo message không bị mất
      acks: all
      retries: 3
    
    # Config cho Consumer (nhận message)
    consumer:
      group-id: post-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      # Đọc từ đầu nếu chưa có offset
      auto-offset-reset: earliest
```

---

### Bước 3: Tạo Event class (định nghĩa format message)

```java
// File: post-service/src/main/java/com/blur/postservice/kafka/event/CommentModerationEvent.java

package com.blur.postservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event gửi đi để yêu cầu AI kiểm tra comment
 * 
 * Khi nào dùng: Sau khi user tạo comment mới
 * Gửi tới topic: comment-moderation-request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentModerationEvent {
    
    private String eventId;      // ID unique cho mỗi event (dùng UUID)
    private String commentId;    // ID của comment trong MongoDB
    private String content;      // Nội dung comment cần check
    private String authorId;     // ID người viết comment
    private String postId;       // ID bài post
    private Long timestamp;      // Thời gian tạo event
    
}
```

```java
// File: post-service/src/main/java/com/blur/postservice/kafka/event/ModerationResultEvent.java

package com.blur.postservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event nhận về sau khi AI xử lý xong
 * 
 * Nhận từ topic: comment-moderation-result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResultEvent {
    
    private String eventId;      // ID của event
    private String commentId;    // ID comment đã check
    private Boolean isToxic;     // true = toxic, false = OK
    private Double confidence;   // Độ tin cậy (0.0 - 1.0)
    private String action;       // "APPROVE" hoặc "REJECT"
    private String reason;       // Lý do nếu reject (spam, hate, violence...)
    
}
```

---

### Bước 4: Tạo Producer (gửi event)

```java
// File: post-service/src/main/java/com/blur/postservice/kafka/producer/CommentEventProducer.java

package com.blur.postservice.kafka.producer;

import com.blur.postservice.kafka.event.CommentModerationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Producer: Gửi event tới Kafka
 * 
 * Cách hoạt động:
 * 1. Nhận thông tin comment
 * 2. Tạo event object
 * 3. Convert sang JSON string
 * 4. Gửi tới Kafka topic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventProducer {
    
    // KafkaTemplate được Spring tự động inject
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    // ObjectMapper để convert object -> JSON
    private final ObjectMapper objectMapper;
    
    // Tên topic - nên đặt ở constant hoặc config
    private static final String TOPIC_MODERATION_REQUEST = "comment-moderation-request";
    
    /**
     * Gửi comment để AI kiểm tra
     * 
     * @param commentId - ID của comment
     * @param content - Nội dung comment
     * @param authorId - ID người viết
     * @param postId - ID bài post
     */
    public void sendForModeration(String commentId, String content, 
                                   String authorId, String postId) {
        try {
            // 1. Tạo event object
            CommentModerationEvent event = CommentModerationEvent.builder()
                .eventId(UUID.randomUUID().toString())  // Tạo ID unique
                .commentId(commentId)
                .content(content)
                .authorId(authorId)
                .postId(postId)
                .timestamp(System.currentTimeMillis())
                .build();
            
            // 2. Convert sang JSON
            String jsonMessage = objectMapper.writeValueAsString(event);
            
            // 3. Gửi tới Kafka
            // send(topic, key, value)
            // - topic: tên topic
            // - key: dùng commentId để các message cùng comment vào cùng partition
            // - value: nội dung message (JSON string)
            kafkaTemplate.send(TOPIC_MODERATION_REQUEST, commentId, jsonMessage);
            
            log.info("✅ Đã gửi comment {} để kiểm tra toxic", commentId);
            
        } catch (Exception e) {
            log.error("❌ Lỗi gửi event: {}", e.getMessage());
            // TODO: Xử lý lỗi (có thể lưu vào DB để retry sau)
        }
    }
}
```

---

### Bước 5: Tạo Consumer (nhận kết quả)

```java
// File: post-service/src/main/java/com/blur/postservice/kafka/consumer/ModerationResultConsumer.java

package com.blur.postservice.kafka.consumer;

import com.blur.postservice.kafka.event.ModerationResultEvent;
import com.blur.postservice.entity.Comment;
import com.blur.postservice.entity.CommentStatus;
import com.blur.postservice.repository.CommentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer: Nhận kết quả từ AI Service
 * 
 * Cách hoạt động:
 * 1. Lắng nghe topic "comment-moderation-result"
 * 2. Khi có message mới → method được gọi tự động
 * 3. Parse JSON thành object
 * 4. Update status của comment trong database
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationResultConsumer {
    
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Method này được gọi tự động khi có message mới
     * 
     * @KafkaListener - đánh dấu đây là consumer
     * - topics: topic cần lắng nghe
     * - groupId: tên nhóm consumer (quan trọng cho việc scale)
     */
    @KafkaListener(
        topics = "comment-moderation-result",
        groupId = "post-service-moderation"
    )
    public void handleModerationResult(String message) {
        log.info("📩 Nhận được kết quả moderation: {}", message);
        
        try {
            // 1. Parse JSON thành object
            ModerationResultEvent event = objectMapper.readValue(
                message, 
                ModerationResultEvent.class
            );
            
            // 2. Tìm comment trong database
            Comment comment = commentRepository.findById(event.getCommentId())
                .orElse(null);
            
            if (comment == null) {
                log.warn("⚠️ Không tìm thấy comment: {}", event.getCommentId());
                return;
            }
            
            // 3. Update status dựa trên kết quả AI
            if (event.getIsToxic()) {
                // Toxic → Reject
                comment.setStatus(CommentStatus.REJECTED);
                comment.setRejectionReason(event.getReason());
                log.info("🚫 Comment {} bị reject: {}", event.getCommentId(), event.getReason());
            } else {
                // OK → Approve
                comment.setStatus(CommentStatus.APPROVED);
                log.info("✅ Comment {} được approve", event.getCommentId());
            }
            
            // 4. Lưu vào database
            commentRepository.save(comment);
            
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý kết quả: {}", e.getMessage());
            // Throw exception để Kafka retry
            throw new RuntimeException("Failed to process moderation result", e);
        }
    }
}
```

---

### Bước 6: Thêm enum CommentStatus

```java
// File: post-service/src/main/java/com/blur/postservice/entity/CommentStatus.java

package com.blur.postservice.entity;

/**
 * Trạng thái của comment trong quá trình moderation
 */
public enum CommentStatus {
    PENDING,    // Đang chờ AI xử lý
    APPROVED,   // Đã được duyệt
    REJECTED    // Bị từ chối (toxic)
}
```

---

### Bước 7: Update Comment Entity

```java
// Thêm field vào Comment entity của bạn

@Document(collection = "comments")
public class Comment {
    
    @Id
    private String id;
    
    private String content;
    private String authorId;
    private String postId;
    private LocalDateTime createdAt;
    
    // 👇 THÊM MỚI CHO MODERATION
    private CommentStatus status = CommentStatus.PENDING;  // Mặc định là PENDING
    private String rejectionReason;  // Lý do nếu bị reject
    
    // ... getters, setters, constructor ...
}
```

---

### Bước 8: Update CommentService

```java
// File: post-service/src/main/java/com/blur/postservice/service/CommentService.java

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final CommentEventProducer eventProducer;  // 👈 Inject producer
    
    /**
     * Tạo comment mới
     * 
     * Flow:
     * 1. Lưu comment với status = PENDING
     * 2. Gửi event tới Kafka để AI kiểm tra
     * 3. Return comment cho user (user thấy comment ngay)
     * 4. AI xử lý async → update status sau
     */
    public Comment createComment(CreateCommentRequest request) {
        
        // 1. Tạo comment với status PENDING
        Comment comment = Comment.builder()
            .content(request.getContent())
            .authorId(request.getAuthorId())
            .postId(request.getPostId())
            .status(CommentStatus.PENDING)  // 👈 Quan trọng
            .createdAt(LocalDateTime.now())
            .build();
        
        // 2. Lưu vào database
        comment = commentRepository.save(comment);
        log.info("💾 Đã lưu comment: {}", comment.getId());
        
        // 3. Gửi event để AI kiểm tra (async - không chờ)
        eventProducer.sendForModeration(
            comment.getId(),
            comment.getContent(),
            comment.getAuthorId(),
            comment.getPostId()
        );
        
        // 4. Return ngay cho user
        // User thấy comment với status PENDING
        // Khi AI xong → status tự update
        return comment;
    }
    
    /**
     * Lấy comments (chỉ lấy những comment đã APPROVED)
     */
    public List<Comment> getApprovedComments(String postId) {
        return commentRepository.findByPostIdAndStatus(postId, CommentStatus.APPROVED);
    }
}
```

---

### Bước 9: Test thử

```java
// Cách test nhanh:

// 1. Chạy Kafka
// docker-compose up -d kafka zookeeper

// 2. Tạo 1 REST endpoint để test
@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private CommentEventProducer producer;
    
    @PostMapping("/send")
    public String testSend() {
        producer.sendForModeration(
            "comment-123",
            "Đây là comment test",
            "user-456",
            "post-789"
        );
        return "Đã gửi!";
    }
}

// 3. Gọi API: POST http://localhost:8080/test/send
// 4. Xem console log của cả 2 service
```

---

### Tóm tắt: Flow đầy đủ

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FLOW ĐẦY ĐỦ - POST SERVICE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   👤 User                                                                    │
│      │                                                                       │
│      │ POST /comments {content: "Hello world!"}                             │
│      ▼                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  CommentController.createComment()                                   │   │
│   └───────────────────────────────┬─────────────────────────────────────┘   │
│                                   │                                          │
│                                   ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  CommentService.createComment()                                      │   │
│   │                                                                       │   │
│   │  1️⃣ Tạo Comment với status = PENDING                                │   │
│   │  2️⃣ Lưu vào MongoDB                                                 │   │
│   │  3️⃣ Gọi eventProducer.sendForModeration()                           │   │
│   │  4️⃣ Return comment cho user                                         │   │
│   └───────────────────────────────┬─────────────────────────────────────┘   │
│                                   │                                          │
│       ┌───────────────────────────┴───────────────────────────┐             │
│       │                                                        │             │
│       ▼ (Sync - User nhận response)              ▼ (Async)     │             │
│   ┌───────────┐                      ┌─────────────────────┐   │             │
│   │  Response │                      │ CommentEventProducer│   │             │
│   │  200 OK   │                      │                     │   │             │
│   │  {comment}│                      │ kafkaTemplate.send()│   │             │
│   └───────────┘                      └──────────┬──────────┘   │             │
│                                                  │              │             │
│                                                  ▼              │             │
│                        ┌─────────────────────────────────────┐  │             │
│                        │  KAFKA TOPIC                        │  │             │
│                        │  "comment-moderation-request"       │  │             │
│                        │  Message: {commentId, content, ...} │  │             │
│                        └──────────────────┬──────────────────┘  │             │
│                                           │                     │             │
│                                           ▼                     │             │
│                        ┌─────────────────────────────────────┐  │             │
│                        │  AI SERVICE (Python)                │  │             │
│                        │  PhoBERT check toxic                │  │             │
│                        └──────────────────┬──────────────────┘  │             │
│                                           │                     │             │
│                                           ▼                     │             │
│                        ┌─────────────────────────────────────┐  │             │
│                        │  KAFKA TOPIC                        │  │             │
│                        │  "comment-moderation-result"        │  │             │
│                        │  Message: {commentId, isToxic, ...} │  │             │
│                        └──────────────────┬──────────────────┘  │             │
│                                           │                     │             │
│                                           ▼                     │             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  ModerationResultConsumer.handleModerationResult()                   │   │
│   │                                                                       │   │
│   │  1️⃣ Parse JSON thành object                                         │   │
│   │  2️⃣ Tìm comment trong MongoDB                                       │   │
│   │  3️⃣ Update status = APPROVED hoặc REJECTED                          │   │
│   │  4️⃣ Lưu lại vào MongoDB                                             │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   💡 User không cần chờ AI xử lý!                                           │
│   💡 Comment hiển thị ngay với status PENDING                               │
│   💡 Status tự động cập nhật khi AI xong                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## �🐍 AI SERVICE (Python) - Đơn giản

```python
# ai-service/main.py
from fastapi import FastAPI
from kafka import KafkaConsumer, KafkaProducer
import json

app = FastAPI()

# Load model khi khởi động
from model import ToxicClassifier
classifier = ToxicClassifier()

# Consumer chạy trong background
def consume_messages():
    consumer = KafkaConsumer(
        'comment-moderation-request',
        bootstrap_servers='localhost:9092',
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )
    
    producer = KafkaProducer(
        bootstrap_servers='localhost:9092',
        value_serializer=lambda m: json.dumps(m).encode('utf-8')
    )
    
    for message in consumer:
        event = message.value
        content = event['content']
        comment_id = event['commentId']
        
        # Predict
        is_toxic = classifier.predict(content)
        
        # Gửi kết quả
        result = {
            'commentId': comment_id,
            'isToxic': is_toxic,
            'action': 'REJECT' if is_toxic else 'APPROVE'
        }
        producer.send('comment-moderation-result', result)
        print(f"✅ Processed: {comment_id} → {result['action']}")

@app.get("/health")
def health():
    return {"status": "ok"}
```

---

## ⚙️ CẤU HÌNH ĐƠN GIẢN

```yaml
# application.yml của mỗi service
spring:
  kafka:
    bootstrap-servers: localhost:9092
    
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

---

## 📝 CHECKLIST TIẾN ĐỘ

### Mục tiêu ngắn hạn (4 tuần đầu)
- [ ] Chạy được Kafka với Docker
- [ ] Gửi được 1 message từ Producer
- [ ] Nhận được message ở Consumer
- [ ] Hiểu code notification-service hiện có

### Mục tiêu trung hạn (4 tuần tiếp)
- [ ] Tạo flow comment → AI moderation
- [ ] AI Service nhận và xử lý message
- [ ] Update comment status sau khi AI check
- [ ] Xử lý retry khi có lỗi

### Mục tiêu dài hạn (sau 8 tuần)
- [ ] Áp dụng pattern cho các service khác
- [ ] Thêm monitoring (xem message lag)
- [ ] Tối ưu performance

---

## 🔗 TÀI LIỆU HỌC THÊM

| Nguồn | Link | Gợi ý |
|-------|------|-------|
| **YouTube** | "Kafka Tutorial for Beginners" | Xem video tiếng Việt/Anh đều được |
| **Baeldung** | baeldung.com/spring-kafka | Hướng dẫn Spring + Kafka chi tiết |
| **Kafka Docs** | kafka.apache.org/quickstart | Quickstart chính thức |
| **Code hiện có** | `/notification-service/kafka/` | Học từ code trong project |

---

## � TỐI ƯU CODEBASE: COMMON LIBRARY (blur-common-lib)

> **Vấn đề:** Mỗi service đang copy paste code giống nhau (DTO, Exception, Config...)
> **Giải pháp:** Tạo 1 module chung, các service khác import vào

### Cấu trúc thư mục Common Library

```
Backend/
├── blur-common-lib/                    # 👈 MODULE MỚI
│   ├── pom.xml
│   └── src/main/java/com/blur/common/
│       │
│       ├── dto/                        # DTO dùng chung
│       │   ├── ApiResponse.java        # Response wrapper
│       │   └── PageResponse.java       # Phân trang
│       │
│       ├── exception/                  # Exception dùng chung
│       │   ├── BlurException.java      # Base exception
│       │   ├── ResourceNotFoundException.java
│       │   └── GlobalExceptionHandler.java
│       │
│       ├── event/                      # Kafka event base
│       │   └── BaseEvent.java
│       │
│       ├── security/                   # Security utilities
│       │   ├── JwtTokenProvider.java
│       │   └── CookieUtils.java
│       │
│       └── kafka/                      # Kafka utilities  
│           └── IdempotentConsumerService.java
│
├── IdentityService/                    # Import blur-common-lib
├── post-service/                       # Import blur-common-lib
├── notification-service/               # Import blur-common-lib
└── ... (các service khác)
```

---

### Bước 1: Tạo pom.xml cho Common Library

```xml
<!-- blur-common-lib/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.blur</groupId>
    <artifactId>blur-common-lib</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>Blur Common Library</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
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
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

---

### Bước 2: Tạo các class dùng chung

```java
// File: blur-common-lib/src/main/java/com/blur/common/dto/ApiResponse.java

package com.blur.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper chung cho tất cả API
 * 
 * Sử dụng: return ApiResponse.success(data);
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Bỏ field null khi serialize
public class ApiResponse<T> {
    
    @Builder.Default
    private int code = 200;
    
    private String message;
    private T result;
    
    // Helper methods
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("Success")
            .result(data)
            .build();
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .build();
    }
}
```

```java
// File: blur-common-lib/src/main/java/com/blur/common/exception/BlurException.java

package com.blur.common.exception;

import lombok.Getter;

/**
 * Base exception cho toàn hệ thống
 */
@Getter
public class BlurException extends RuntimeException {
    
    private final int errorCode;
    
    public BlurException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

```java
// File: blur-common-lib/src/main/java/com/blur/common/exception/ResourceNotFoundException.java

package com.blur.common.exception;

/**
 * Throw khi không tìm thấy resource (user, post, comment...)
 */
public class ResourceNotFoundException extends BlurException {
    
    public ResourceNotFoundException(String resource, String id) {
        super(404, resource + " not found with id: " + id);
    }
}
```

```java
// File: blur-common-lib/src/main/java/com/blur/common/exception/GlobalExceptionHandler.java

package com.blur.common.exception;

import com.blur.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Xử lý exception tập trung cho tất cả controller
 * 
 * Các service import class này và thêm @Import(GlobalExceptionHandler.class)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BlurException.class)
    public ResponseEntity<ApiResponse<?>> handleBlurException(BlurException e) {
        log.error("BlurException: {}", e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode())
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error(500, "Internal server error"));
    }
}
```

```java
// File: blur-common-lib/src/main/java/com/blur/common/event/BaseEvent.java

package com.blur.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class cho tất cả Kafka event
 * 
 * Các event khác extends class này
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    private String eventId;       // UUID unique
    private String eventType;     // VD: "comment.created"
    private Long timestamp;       // Epoch milliseconds
    private String sourceService; // Service tạo event
    
}
```

---

### Bước 3: Import vào các service khác

```xml
<!-- Thêm vào pom.xml của post-service, notification-service, etc. -->
<dependencies>
    <!-- ... các dependency khác ... -->
    
    <!-- Common Library -->
    <dependency>
        <groupId>com.blur</groupId>
        <artifactId>blur-common-lib</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

```java
// Sử dụng trong code
import com.blur.common.dto.ApiResponse;
import com.blur.common.exception.ResourceNotFoundException;

@RestController
public class PostController {
    
    @GetMapping("/posts/{id}")
    public ApiResponse<Post> getPost(@PathVariable String id) {
        Post post = postService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", id));
        
        return ApiResponse.success(post);
    }
}
```

---

### Bước 4: Build và install common library

```bash
# Vào thư mục blur-common-lib
cd Backend/blur-common-lib

# Build và install vào local Maven repository
mvn clean install

# Sau đó các service khác có thể import được
```

---

## 🔐 LƯU JWT TOKEN VÀO COOKIE

> **Vấn đề hiện tại:** Token lưu trong localStorage → Dễ bị XSS attack
> **Giải pháp:** Lưu token trong HttpOnly Cookie → JavaScript không đọc được

### Tại sao dùng Cookie thay vì localStorage?

```
┌────────────────────────────────────────────────────────────────────┐
│                 SO SÁNH: localStorage vs Cookie                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   localStorage:                                                     │
│   ┌─────────────────────────────────────────┐                      │
│   │ ❌ JavaScript có thể đọc được           │                      │
│   │ ❌ Nếu bị XSS → hacker lấy được token   │                      │
│   │ ❌ Phải tự gửi token trong header       │                      │
│   └─────────────────────────────────────────┘                      │
│                                                                     │
│   HttpOnly Cookie:                                                  │
│   ┌─────────────────────────────────────────┐                      │
│   │ ✅ JavaScript KHÔNG đọc được            │                      │
│   │ ✅ Browser tự động gửi cookie           │                      │
│   │ ✅ An toàn hơn với XSS                  │                      │
│   └─────────────────────────────────────────┘                      │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

### Bước 1: Tạo CookieUtils trong Common Library

```java
// File: blur-common-lib/src/main/java/com/blur/common/security/CookieUtils.java

package com.blur.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class để thao tác với Cookie
 */
@Component
public class CookieUtils {
    
    // Tên cookie
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    
    /**
     * Tạo cookie chứa access token
     * 
     * @param token - JWT access token
     * @param maxAgeSeconds - Thời gian sống của cookie (giây)
     */
    public ResponseCookie createAccessTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
            .httpOnly(true)           // 👈 JavaScript không đọc được
            .secure(true)             // 👈 Chỉ gửi qua HTTPS
            .sameSite("Strict")       // 👈 Chống CSRF
            .path("/")                // Cookie áp dụng cho tất cả path
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }
    
    /**
     * Tạo cookie chứa refresh token
     * 
     * Path giới hạn chỉ cho endpoint refresh
     */
    public ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth/refresh")  // 👈 Chỉ gửi khi gọi refresh endpoint
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }
    
    /**
     * Xóa cookie (khi logout)
     */
    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)  // 👈 maxAge = 0 để xóa cookie
            .build();
    }
    
    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/auth/refresh")
            .maxAge(0)
            .build();
    }
    
    /**
     * Lấy token từ cookie trong request
     */
    public Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        
        return Arrays.stream(request.getCookies())
            .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
    
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        
        return Arrays.stream(request.getCookies())
            .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}
```

---

### Bước 2: Sửa AuthController (IdentityService)

```java
// File: IdentityService/src/main/java/.../controller/AuthController.java

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    
    // Thời gian sống của token (giây)
    private static final long ACCESS_TOKEN_EXPIRY = 15 * 60;        // 15 phút
    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60; // 7 ngày
    
    /**
     * Login - Trả về token trong Cookie thay vì response body
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserInfo>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        
        // 1. Xác thực user
        AuthResult authResult = authService.login(request);
        
        // 2. Tạo tokens
        String accessToken = jwtTokenProvider.generateAccessToken(authResult.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(authResult.getUserId());
        
        // 3. Đặt token vào Cookie (KHÔNG trả về trong response body)
        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(
            accessToken, ACCESS_TOKEN_EXPIRY);
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(
            refreshToken, REFRESH_TOKEN_EXPIRY);
        
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        
        // 4. Chỉ trả về user info (KHÔNG có token)
        return ResponseEntity.ok(ApiResponse.success(authResult.getUserInfo()));
    }
    
    /**
     * Refresh token - Lấy refresh token từ Cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 1. Lấy refresh token từ cookie
        String refreshToken = cookieUtils.getRefreshTokenFromCookie(request)
            .orElseThrow(() -> new BlurException(401, "Refresh token not found"));
        
        // 2. Verify và tạo token mới
        String userId = jwtTokenProvider.validateRefreshToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
        
        // 3. Set cookie mới
        response.addHeader("Set-Cookie", 
            cookieUtils.createAccessTokenCookie(newAccessToken, ACCESS_TOKEN_EXPIRY).toString());
        response.addHeader("Set-Cookie", 
            cookieUtils.createRefreshTokenCookie(newRefreshToken, REFRESH_TOKEN_EXPIRY).toString());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * Logout - Xóa cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        
        // Xóa cookie bằng cách set maxAge = 0
        response.addHeader("Set-Cookie", cookieUtils.deleteAccessTokenCookie().toString());
        response.addHeader("Set-Cookie", cookieUtils.deleteRefreshTokenCookie().toString());
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
```

---

### Bước 3: Sửa JwtAuthenticationFilter

```java
// File: IdentityService/src/main/java/.../config/JwtAuthenticationFilter.java

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Lấy token từ Cookie (thay vì Header)
            Optional<String> tokenOpt = cookieUtils.getAccessTokenFromCookie(request);
            
            if (tokenOpt.isEmpty()) {
                // Không có token → cho đi tiếp (sẽ bị chặn ở security config nếu cần auth)
                filterChain.doFilter(request, response);
                return;
            }
            
            String token = tokenOpt.get();
            
            // 2. Validate token
            if (jwtTokenProvider.validateToken(token)) {
                // 3. Lấy user info và set vào SecurityContext
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

### Bước 4: Cấu hình Security (cho phép Cookie)

```java
// File: IdentityService/src/main/java/.../config/SecurityConfig.java

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // Tắt CSRF cho API (vì dùng SameSite=Strict)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Cho phép CORS với credentials (cần cho cookie)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorize requests
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            
            // Thêm JWT filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Cho phép frontend origin
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        
        // 👇 QUAN TRỌNG: Cho phép gửi cookie
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

### Bước 5: Sửa Frontend (React/Vue)

```javascript
// Frontend - Cấu hình Axios để gửi cookie

import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080',
    
    // 👇 QUAN TRỌNG: Gửi cookie cùng với request
    withCredentials: true,
});

// Interceptor xử lý refresh token tự động
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        
        // Nếu lỗi 401 và chưa retry
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            
            try {
                // Gọi refresh endpoint (cookie tự động được gửi)
                await axios.post('http://localhost:8080/api/auth/refresh', {}, {
                    withCredentials: true
                });
                
                // Retry request gốc
                return api(originalRequest);
                
            } catch (refreshError) {
                // Refresh thất bại → logout
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }
        
        return Promise.reject(error);
    }
);

export default api;
```

```javascript
// Sử dụng trong component

import api from './api';

// Login
const login = async (email, password) => {
    const response = await api.post('/api/auth/login', { email, password });
    // Token được browser tự động lưu vào cookie
    // Không cần làm gì thêm!
    return response.data.result; // UserInfo
};

// Gọi API có auth
const getProfile = async () => {
    // Cookie tự động được gửi
    const response = await api.get('/api/profile/me');
    return response.data.result;
};

// Logout
const logout = async () => {
    await api.post('/api/auth/logout');
    window.location.href = '/login';
};
```

---

### Tóm tắt Flow xác thực với Cookie

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AUTHENTICATION FLOW VỚI COOKIE                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1️⃣ LOGIN                                                                  │
│     User ─── POST /login {email, password} ───▶ Server                      │
│     User ◀── Set-Cookie: access_token=xxx; HttpOnly; Secure ─── Server      │
│     User ◀── Set-Cookie: refresh_token=xxx; HttpOnly; Secure ─── Server     │
│                                                                              │
│     💡 Token được lưu trong cookie, không lộ ra JavaScript                  │
│                                                                              │
│  2️⃣ GỌI API                                                                │
│     User ─── GET /api/profile (Cookie tự động gửi) ───▶ Server              │
│     User ◀── {profile data} ─── Server                                      │
│                                                                              │
│     �� Browser tự động gửi cookie, không cần code gì thêm                   │
│                                                                              │
│  3️⃣ TOKEN HẾT HẠN                                                          │
│     User ─── GET /api/posts ───▶ Server                                     │
│     User ◀── 401 Unauthorized ─── Server                                    │
│                                                                              │
│     Frontend tự động gọi refresh:                                           │
│     User ─── POST /refresh (refresh_token cookie) ───▶ Server               │
│     User ◀── Set-Cookie: (new tokens) ─── Server                            │
│                                                                              │
│     Frontend retry request gốc → thành công                                 │
│                                                                              │
│  4️⃣ LOGOUT                                                                 │
│     User ─── POST /logout ───▶ Server                                       │
│     User ◀── Set-Cookie: access_token=; Max-Age=0 ─── Server                │
│                                                                              │
│     💡 Cookie bị xóa                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📝 CHECKLIST TIẾN ĐỘ (ĐẦY ĐỦ)

### Kafka cơ bản (4 tuần)
- [ ] Chạy được Kafka với Docker
- [ ] Gửi/nhận message đơn giản
- [ ] Hiểu notification-service hiện có

### Kafka nâng cao (4 tuần)
- [ ] Tạo flow comment → AI moderation
- [ ] Xử lý retry và DLQ
- [ ] Idempotent consumer

### Common Library (1 tuần)
- [ ] Tạo module blur-common-lib
- [ ] Move DTO, Exception vào common
- [ ] Các service import common-lib

### JWT Cookie (1 tuần)
- [ ] Tạo CookieUtils
- [ ] Sửa AuthController (set cookie)
- [ ] Sửa JwtFilter (đọc từ cookie)
- [ ] Sửa Frontend (withCredentials)

---

## 💡 MẸO CHO SINH VIÊN

1. **Đừng cố hiểu hết ngay** - Làm trước, hiểu sau
2. **Console.log / System.out.println là bạn** - Log mọi thứ để debug
3. **Copy code mẫu rồi sửa** - Không cần viết từ đầu
4. **Khi bị stuck > 30 phút** - Hỏi hoặc tìm kiếm, đừng ngồi stuck
5. **Kafka UI** - Cài kafka-ui để xem message trực quan

```yaml
# Thêm vào docker-compose.yml để có giao diện quản lý Kafka
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8080:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9093
```

---

> 💪 **Bắt đầu từ nhỏ, làm từng bước một. Không cần hoàn hảo ngay từ đầu!**

