# Giai Đoạn 8: Keycloak Migration

## Mục tiêu

Chuyển từ custom JWT → Keycloak cho SSO, OAuth2, role management.
Giai đoạn này phức tạp, triển khai CUỐI vì ảnh hưởng tất cả services.

## Bước 1: Thêm Keycloak vào Docker Compose

Thêm đoạn sau vào file `docker-compose.yml`:

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
```

## Bước 2: Setup Keycloak Realm và Clients

### 2.1 Tạo Realm "blur"

1. Truy cập Keycloak Admin Console: `http://localhost:9090`
2. Đăng nhập với `admin` / `admin`
3. Click "Create Realm"
4. Nhập Realm name: `blur`
5. Click "Create"

### 2.2 Tạo Clients

Trong Realm "blur", tạo các clients sau:

**Client 1: api-gateway**
1. Clients → Create client
2. Client ID: `api-gateway`
3. Client authentication: ON
4. Authorization: OFF
5. Authentication flow: chỉ bật "Service accounts roles"
6. Valid redirect URIs: `http://localhost:8888/*`
7. Web origins: `http://localhost:3000`

**Client 2: blur-frontend**
1. Clients → Create client
2. Client ID: `blur-frontend`
3. Client authentication: OFF (public client)
4. Authentication flow: bật "Standard flow", "Direct access grants"
5. Valid redirect URIs: `http://localhost:3000/*`
6. Web origins: `http://localhost:3000`

### 2.3 Tạo Roles

1. Realm roles → Create role
2. Tạo các roles: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR`

### 2.4 Tạo User mẫu

1. Users → Add user
2. Username: `testuser`
3. Email: `test@blur.com`
4. Email verified: ON
5. Click Create
6. Tab Credentials → Set password: `password123`, Temporary: OFF
7. Tab Role mapping → Assign role: `ROLE_USER`

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

## Bước 4: Cập nhật API Gateway Security Config

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
            "/api/identity/auth/**",
            "/api/identity/users/registration",
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

## Bước 5: Cấu hình application.yaml cho Keycloak

**File:** `api-gateway/src/main/resources/application.yaml`

Thêm cấu hình sau:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9090/realms/blur}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:9090/realms/blur/protocol/openid-connect/certs}
```

## Bước 6: Tạo JWT Role Converter (ánh xạ Keycloak roles → Spring Security)

**File:** `api-gateway/src/main/java/com/blur/apigateway/configuration/KeycloakRoleConverter.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
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

## Bước 7: Script migrate users từ MySQL sang Keycloak

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

# Đọc users từ MySQL và tạo trong Keycloak
# Thay đổi connection string cho phù hợp
mysql -h localhost -u root -proot blur_identity -N -e "SELECT username, email, first_name, last_name FROM users" | while IFS=$'\t' read -r username email first_name last_name; do
  echo "Creating user: $username"

  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"$username\",
      \"email\": \"$email\",
      \"firstName\": \"$first_name\",
      \"lastName\": \"$last_name\",
      \"enabled\": true,
      \"emailVerified\": true
    }"

  # Lấy user ID vừa tạo
  USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$username" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

  # Set password (temporary = false)
  curl -s -X PUT "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/reset-password" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "type": "password",
      "value": "changeme123",
      "temporary": false
    }'

  # Assign ROLE_USER
  ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/ROLE_USER" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')

  curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/role-mappings/realm" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[{\"id\": \"$ROLE_ID\", \"name\": \"ROLE_USER\"}]"

  echo "User $username created and assigned ROLE_USER"
done

echo "Migration completed!"
```

## Hướng dẫn Test

### Test 1: Keycloak đang chạy

```bash
# Khởi động Keycloak
docker-compose up -d keycloak

# Kiểm tra Keycloak health
curl -s http://localhost:9090/health
```

Truy cập `http://localhost:9090` → thấy Keycloak Admin Console.

### Test 2: Lấy token từ Keycloak

```bash
curl -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=blur-frontend" \
  -d "username=testuser" \
  -d "password=password123"
```

Response mong đợi:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "abc-123-def",
  "scope": "openid email profile"
}
```

### Test 3: Dùng Keycloak token gọi API qua Gateway

```bash
# Lấy token
TOKEN=$(curl -s -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=blur-frontend" \
  -d "username=testuser" \
  -d "password=password123" | jq -r '.access_token')

# Gọi API với Keycloak token
curl -X GET http://localhost:8888/api/profile/users/my-profile \
  -H "Authorization: Bearer $TOKEN"
```

Response mong đợi: trả về profile của testuser (code 1000).

### Test 4: Gọi API không có token → bị chặn

```bash
curl -X GET http://localhost:8888/api/profile/users/my-profile
```

Response mong đợi: 401 Unauthorized.

### Test 5: Gọi public endpoint không cần token

```bash
curl -X GET http://localhost:8888/actuator/health
```

Response mong đợi: 200 OK.

### Test 6: Token refresh

```bash
# Lấy refresh_token từ Test 2
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI..."

curl -X POST http://localhost:9090/realms/blur/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "client_id=blur-frontend" \
  -d "refresh_token=$REFRESH_TOKEN"
```

Response mong đợi: trả về access_token mới.

### Test 7: Decode JWT để xem roles

```bash
# Decode JWT payload (phần giữa, tách bởi dấu chấm)
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

Mong đợi thấy:
```json
{
  "realm_access": {
    "roles": ["ROLE_USER"]
  },
  "preferred_username": "testuser",
  "email": "test@blur.com"
}
```

## Checklist

- [ ] Setup Keycloak container + tạo database keycloak trong MySQL
- [ ] Tạo Realm "blur" trong Keycloak Admin Console
- [ ] Tạo Client "api-gateway" (confidential) và "blur-frontend" (public)
- [ ] Tạo Realm Roles: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
- [ ] Tạo user mẫu và assign role
- [ ] Thêm OAuth2 Resource Server dependency vào api-gateway
- [ ] Cập nhật SecurityConfig.java → dùng oauth2ResourceServer
- [ ] Thêm Keycloak issuer-uri và jwk-set-uri vào application.yaml
- [ ] Tạo KeycloakRoleConverter để ánh xạ roles
- [ ] Chạy script migrate users từ MySQL → Keycloak
- [ ] Cập nhật tất cả services validate Keycloak JWT thay vì custom JWT
- [ ] Test: lấy token từ Keycloak thành công
- [ ] Test: dùng Keycloak token gọi API qua Gateway thành công
- [ ] Test: gọi API không token → 401 Unauthorized
- [ ] Test: public endpoints vẫn truy cập được
- [ ] Test: token refresh hoạt động
