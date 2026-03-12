# Giai Đoạn 8: Keycloak Migration (CHƯA TRIỂN KHAI)

## Mục tiêu

Chuyển từ custom JWT (HS512 signed, self-managed) → Keycloak cho SSO, OAuth2, role management.
Giai đoạn này ảnh hưởng TẤT CẢ services nên triển khai cuối.

## Trạng thái: CHƯA TRIỂN KHAI

## Authentication hiện tại

### Cách hoạt động hiện tại

```
Frontend                  API Gateway (8888)           User Service (8081)
────────                 ────────────────────         ─────────────────────
POST /auth/token    →    AuthenticationFilter    →    AuthController.authenticate()
  {username, password}     │                              │
                           │ Extract token from:          │ Verify username/password
                           │  - Cookie (priority)         │ Generate JWT (HS512)
                           │  - Authorization header      │ Set HttpOnly cookie
                           │  - Query param               │ Return token
                           │                              │
GET /api/post/...   →    AuthenticationFilter            │
  Cookie: token            │                              │
                           │ Introspect token ───────→   AuthController.introspect()
                           │ via Feign ProfileAuthClient   │ Verify JWT signature
                           │                              │ Check expiration
                           │ Token valid → forward        │ Return {valid, userId}
                           │ Token invalid → 401          │
```

### Vấn đề

1. **JWT self-signed (HS512):** Tất cả services phải share cùng `SIGNER_KEY` → nếu leak, toàn bộ hệ thống compromised
2. **Không có SSO:** Mỗi service tự validate, không có central identity provider
3. **Không có RBAC chuẩn:** Roles lưu trong UserProfile Neo4j node, không có fine-grained permissions
4. **Token revocation thủ công:** Dùng Redis blacklist (`RedisService.invalidateToken()`) - không scale
5. **Google OAuth tự implement:** `OutboundIdentityClient` gọi trực tiếp Google API

## Bước 1: Thêm Keycloak vào Docker Compose

**File:** `Backend/docker-compose.yml`

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  command: start-dev
  environment:
    KC_DB: mysql
    KC_DB_URL: jdbc:mysql://mysql:3306/keycloak
    KC_DB_USERNAME: root
    KC_DB_PASSWORD: root
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  ports:
    - "9090:8080"
  depends_on:
    - mysql

mysql:
  image: mysql:8.0
  environment:
    MYSQL_ROOT_PASSWORD: root
    MYSQL_DATABASE: keycloak
  ports:
    - "3306:3306"
  volumes:
    - mysql_data:/var/lib/mysql
```

## Bước 2: Setup Keycloak Realm và Clients

### 2.1 Tạo Realm "blur"

1. Truy cập `http://localhost:9090` → đăng nhập `admin/admin`
2. Create Realm → Name: `blur`

### 2.2 Tạo Clients

**Client 1: api-gateway** (confidential)
- Client ID: `api-gateway`
- Client authentication: ON
- Authentication flow: Service accounts roles
- Valid redirect URIs: `http://localhost:8888/*`

**Client 2: blur-frontend** (public)
- Client ID: `blur-frontend`
- Client authentication: OFF
- Authentication flow: Standard flow + Direct access grants
- Valid redirect URIs: `http://localhost:3000/*`
- Web origins: `http://localhost:3000`

### 2.3 Tạo Roles

- `ROLE_USER`
- `ROLE_ADMIN`
- `ROLE_MODERATOR`

## Bước 3: Thêm dependency cho API Gateway

**File:** `api-gateway/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

## Bước 4: Cập nhật API Gateway

### SecurityConfig mới

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/SecurityConfig.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/users/registration**",
            "/api/test-data/**",
            "/api/profile/internal/**",
            "/actuator/health"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );
        return http.build();
    }
}
```

### application.yaml - Keycloak JWT config

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9090/realms/blur}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:9090/realms/blur/protocol/openid-connect/certs}
```

## Bước 5: Tạo KeycloakRoleConverter

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/KeycloakRoleConverter.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || realmAccess.isEmpty()) {
            return Flux.empty();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Flux.empty();
        }

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        return Flux.fromIterable(authorities);
    }
}
```

## Bước 6: Script migrate users từ Neo4j sang Keycloak

**File:** `scripts/migrate-users-to-keycloak.sh`

```bash
#!/bin/bash

KEYCLOAK_URL="http://localhost:9090"
REALM="blur"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Lấy admin token
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

echo "Admin token obtained"

# Export users từ Neo4j (dùng cypher-shell hoặc API)
# MATCH (u:user_profile) RETURN u.username, u.email, u.firstName, u.lastName
# Tạo từng user trong Keycloak qua REST API

# Ví dụ tạo 1 user:
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@blur.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true
  }'

echo "Migration completed!"
```

## Bước 7: Cập nhật các services validate Keycloak JWT

Tất cả services (user-service, content-service, communication-service) cần:

1. Thêm dependency `spring-boot-starter-oauth2-resource-server`
2. Xóa `CustomJwtDecoder.java` (không cần nữa, dùng Keycloak JWK)
3. Cập nhật `SecurityConfig.java` → dùng `oauth2ResourceServer()`
4. Cập nhật `application.yaml` → thêm Keycloak issuer-uri

### Thay đổi ở mỗi service

**Xóa:**
- `CustomJwtDecoder.java`
- `AuthenticationService.java` (phần generate/verify JWT)
- `RedisService.invalidateToken()` (Keycloak quản lý token lifecycle)

**Sửa:**
- `SecurityConfig.java` → OAuth2 Resource Server
- `application.yaml` → Keycloak JWT config

**Giữ nguyên:**
- `AuthenticationRequestInterceptor.java` (vẫn propagate Bearer token)
- Tất cả business logic

## Hướng dẫn Test

### Test 1: Lấy token từ Keycloak

```bash
curl -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=blur-frontend" \
  -d "username=testuser" \
  -d "password=password123"
```

### Test 2: Gọi API với Keycloak token

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=blur-frontend" \
  -d "username=testuser" \
  -d "password=password123" | jq -r '.access_token')

curl -X GET http://localhost:8888/api/profile/users/myInfo \
  -H "Authorization: Bearer $TOKEN"
```

### Test 3: Không có token → 401

```bash
curl -X GET http://localhost:8888/api/profile/users/myInfo
# Mong đợi: 401 Unauthorized
```

### Test 4: Token refresh

```bash
curl -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "client_id=blur-frontend" \
  -d "refresh_token=$REFRESH_TOKEN"
```

## Checklist

- [ ] Setup Keycloak container + MySQL database
- [ ] Tạo Realm "blur"
- [ ] Tạo Clients: api-gateway (confidential), blur-frontend (public)
- [ ] Tạo Realm Roles: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
- [ ] Tạo user mẫu và assign role
- [ ] Thêm OAuth2 Resource Server dependency vào api-gateway
- [ ] Tạo SecurityConfig mới với oauth2ResourceServer
- [ ] Thêm Keycloak issuer-uri và jwk-set-uri vào application.yaml
- [ ] Tạo KeycloakRoleConverter
- [ ] Chạy script migrate users từ Neo4j → Keycloak
- [ ] Cập nhật user-service: xóa CustomJwtDecoder, dùng Keycloak JWT
- [ ] Cập nhật content-service: xóa CustomJwtDecoder, dùng Keycloak JWT
- [ ] Cập nhật communication-service: xóa CustomJwtDecoder, dùng Keycloak JWT
- [ ] Cập nhật frontend: login qua Keycloak endpoint thay vì /auth/token
- [ ] Test: lấy token từ Keycloak → gọi API → thành công
- [ ] Test: 401 khi không có token
- [ ] Test: token refresh hoạt động
