# Code full: user-service

## `Backend/user-service/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.blur</groupId>
    <artifactId>user-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>user-service</name>
    <description>user-service</description>
    <url/>
    <licenses>
        <license/>
    </licenses>
    <developers>
        <developer/>
    </developers>
    <scm>
        <connection/>
        <developerConnection/>
        <tag/>
        <url/>
    </scm>
    <properties>
        <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>

        <java.version>21</java.version>
        <jackson.version>2.15.3</jackson.version>

        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <!-- Source: https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt -->
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>10.2</version>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.11.0</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-neo4j</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${org.mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${org.mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>

        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

    </dependencies>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>2024.0.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${org.mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/configuration/JWTAuthenticationEntryPoint.java`
```java
package com.blur.userservice.identity.configuration;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.identity.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/controller/AuthController.java`
```java
package com.blur.userservice.identity.controller;

import com.blur.userservice.identity.dto.request.AuthRequest;
import com.blur.userservice.identity.dto.request.IntrospectRequest;
import com.blur.userservice.identity.dto.request.LogoutRequest;
import com.blur.userservice.identity.dto.request.RefreshRequest;
import com.blur.userservice.identity.dto.response.AuthResponse;
import com.blur.userservice.identity.dto.response.IntrospectResponse;
import com.blur.userservice.identity.service.AuthenticationService;
import com.blur.userservice.identity.util.CookieUtil;
import com.blur.userservice.profile.dto.ApiResponse;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;
    CookieUtil cookieUtil;

    @PostMapping("/token")
    ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        var result = authenticationService.authenticate(authRequest);

        // Set JWT vÃ o HttpOnly Cookie
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        // Tráº£ vá» response KHÃ”NG chá»©a token (hoáº·c cÃ³ thá»ƒ giá»¯ láº¡i náº¿u cáº§n backward compatibility)
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(AuthResponse.builder()
                        .authenticated(result.isAuthenticated())
                        // .token(null)  // KhÃ´ng tráº£ token trong body ná»¯a
                        .build())
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            @RequestBody(required = false) IntrospectRequest introspecRequest)
            throws ParseException, JOSEException {
        // Æ¯u tiÃªn token tá»« cookie, fallback vá» body request (backward compatibility)
        String token = tokenFromCookie != null
                ? tokenFromCookie
                : (introspecRequest != null ? introspecRequest.getToken() : null);

        if (token == null) {
            return ApiResponse.<IntrospectResponse>builder()
                    .result(IntrospectResponse.builder().valid(false).build())
                    .build();
        }

        var result = authenticationService.introspect(
                IntrospectRequest.builder().token(token).build());
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletResponse response)
            throws ParseException, JOSEException {
        String token =
                tokenFromCookie != null ? tokenFromCookie : (logoutRequest != null ? logoutRequest.getToken() : null);

        if (token != null) {
            authenticationService.logout(LogoutRequest.builder().token(token).build());
        }

        // XÃ³a cookies
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil
                        .createLogoutCookie(CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
                        .toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil
                        .createLogoutCookie(CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                        .toString());

        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @CookieValue(name = "access_token", required = false) String accessTokenFromCookie,
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletResponse response)
            throws ParseException, JOSEException {
        // Sá»­ dá»¥ng access_token Ä‘á»ƒ refresh (theo logic hiá»‡n táº¡i cá»§a báº¡n)
        String token = accessTokenFromCookie != null
                ? accessTokenFromCookie
                : (refreshRequest != null ? refreshRequest.getToken() : null);

        var result = authenticationService.refreshToken(
                RefreshRequest.builder().token(token).build());

        // Set cookie má»›i
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        return ApiResponse.<AuthResponse>builder()
                .result(AuthResponse.builder().authenticated(true).build())
                .build();
    }

    // login with google
    @PostMapping("/outbound/authentication")
    ApiResponse<AuthResponse> outboundAuthenticate(@RequestParam("code") String code, HttpServletResponse response) {
        var result = authenticationService.outboundAuthenticationService(code);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(AuthResponse.builder().authenticated(true).build())
                .build();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/controller/TestDataController.java`
```java
package com.blur.userservice.identity.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.service.UserService;
import com.blur.userservice.profile.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-data")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TestDataController {

    private static final String[] FIRST_NAMES = {
            "An", "Binh", "Cuong", "Dung", "Duc", "Giang", "Hai", "Hung", "Khang", "Long",
            "Minh", "Nam", "Phong", "Quang", "Son", "Tam", "Thang", "Tuan", "Viet", "Vu",
            "Anh", "Chi", "Dung", "Hanh", "Huong", "Lan", "Linh", "Mai", "Ngoc", "Phuong",
            "Thao", "Trang", "Trinh", "Uyen", "Yen", "Bao", "Dat", "Hoang", "Khanh", "Thinh"
    };
    private static final String[] LAST_NAMES = {
            "Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo", "Dang",
            "Bui", "Do", "Ho", "Ngo", "Duong", "Ly", "Truong", "Dinh", "Doan", "Luong"
    };

    UserService userService;
    UserProfileRepository userProfileRepository;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generateUsers(@RequestParam(defaultValue = "10000") int userCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            Random random = new Random();
            Set<String> usedUsernames = new HashSet<>();

            for (int i = 0; i < userCount; i++) {
                try {
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String baseUsername = (firstName + lastName).toLowerCase();
                    String username = generateUniqueUsername(baseUsername, usedUsernames);
                    usedUsernames.add(username);

                    UserCreationRequest request = UserCreationRequest.builder()
                            .username(username)
                            .email(username + "@testmail.com")
                            .password("Test@123456")
                            .firstName(firstName)
                            .lastName(lastName)
                            .dob(LocalDate.now().minusYears(20 + random.nextInt(30)))
                            .build();

                    userService.createUser(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("usersCreated", successCount.get());
            result.put("usersFailed", failCount.get());
            result.put("durationMs", duration);
            result.put("durationFormatted", formatDuration(duration));
            result.put("nextStep", "Now call POST /profile/internal/generate-follows to create random follows");

            return ApiResponse.<Map<String, Object>>builder()
                    .code(1000)
                    .message("Users generated! Now call profile-service to create follows.")
                    .result(result)
                    .build();

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("usersCreatedBeforeError", successCount.get());
            return ApiResponse.<Map<String, Object>>builder()
                    .code(5000)
                    .message("Error: " + e.getMessage())
                    .result(result)
                    .build();
        }
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userProfileRepository.count());
        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Stats retrieved")
                .result(result)
                .build();
    }

    private String generateUniqueUsername(String base, Set<String> used) {
        String username = base;
        int counter = 0;
        while (used.contains(username)) {
            username = base + (++counter);
        }
        return username;
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        }
        return String.format("%d sec %d ms", seconds, ms % 1000);
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/controller/UserController.java`
```java
package com.blur.userservice.identity.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.identity.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.identity.mapper.UserMapper;
import com.blur.userservice.identity.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserController {
    UserService userService;
    UserMapper userMapper;

    @PostMapping("/create-password")
    public ApiResponse<Void> createPassword(@RequestBody @Valid UserCreationPasswordRequest request) {
        userService.createPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Password has been created, you could use it to login")
                .build();
    }

    @PostMapping("/registration")
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        var result = userService.createUser(request);
        return ApiResponse.<UserResponse>builder().code(1000).result(result).build();
    }

    @PostMapping("/registrations")
    public ApiResponse<?> createUsers(@RequestBody @Valid UserCreationRequest request) {
        userService.createUser(request);
        return ApiResponse.builder().result(true).build();
    }

    @GetMapping("/all")
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> myInfo() {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable String userId) {
        var userResponse = userMapper.toUserResponse(userService.getUserById(userId));
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userResponse)
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        var updated = userService.updateUser(userId, request);
        return ApiResponse.<UserResponse>builder()
                .result(userMapper.toUserResponse(updated))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/dto/response/UserResponse.java`
```java
package com.blur.userservice.identity.dto.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserResponse {
    String id;
    String username;
    String email;
    boolean emailVerified;
    Boolean noPassword;
    List<String> roles;
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/exception/GlobalExceptionHandler.java`
```java
package com.blur.userservice.identity.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import com.blur.userservice.profile.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
@SuppressWarnings("rawtypes")
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handleRuntimeException(final Exception e) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;

        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = com.blur.userservice.profile.exception.AppException.class)
    ResponseEntity<ApiResponse> handleProfileAppException(com.blur.userservice.profile.exception.AppException e) {
        com.blur.userservice.profile.exception.ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // validation cao cap tu middle tro len
    @SuppressWarnings({"null", "unchecked"})
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String enumKey = Objects.requireNonNull(e.getFieldError()).getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
            var constrainViolation =
                    e.getBindingResult().getAllErrors().getFirst().unwrap(ConstraintViolation.class);
            attributes = constrainViolation.getConstraintDescriptor().getAttributes();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid key: " + enumKey);
        }
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(
                                Objects.nonNull(attributes)
                                        ? mapAttribute(errorCode.getMessage(), attributes)
                                        : errorCode.getMessage())
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/mapper/UserMapper.java`
```java
package com.blur.userservice.identity.mapper;

import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.profile.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    void updateUser(@MappingTarget UserProfile user, UserUpdateRequest userUpdateRequest);

    @Mapping(target = "id", source = "userId")
    @Mapping(target = "noPassword", source = "passwordHash", qualifiedByName = "toNoPassword")
    UserResponse toUserResponse(UserProfile user);

    @Named("toNoPassword")
    default Boolean toNoPassword(String passwordHash) {
        return passwordHash == null || passwordHash.isBlank();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/service/AuthenticationService.java`
```java
package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.AuthRequest;
import com.blur.userservice.identity.dto.request.ExchangeTokenRequest;
import com.blur.userservice.identity.dto.request.IntrospectRequest;
import com.blur.userservice.identity.dto.request.LogoutRequest;
import com.blur.userservice.identity.dto.request.RefreshRequest;
import com.blur.userservice.identity.dto.response.AuthResponse;
import com.blur.userservice.identity.dto.response.IntrospectResponse;
import com.blur.userservice.identity.exception.AppException;
import com.blur.userservice.identity.exception.ErrorCode;
import com.blur.userservice.identity.repository.httpclient.OutboundIdentityClient;
import com.blur.userservice.identity.repository.httpclient.OutboundUserClient;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserProfileRepository userProfileRepository;
    PasswordEncoder passwordEncoder;
    OutboundIdentityClient outboundIdentityClient;
    OutboundUserClient outboundUserClient;
    RedisService redisService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${outbound.identity.client-id}")
    protected String CLIENT_ID;

    @NonFinal
    @Value("${outbound.identity.client-secret}")
    protected String CLIENT_SECRET;

    @NonFinal
    @Value("${outbound.identity.redirect-url}")
    protected String REDIRECT_URL;

    @NonFinal
    @Value("${outbound.identity.grant-type}")
    protected String GRANT_TYPE;

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest authRequest) {
        var user = userProfileRepository
                .findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean authenticated = passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        redisService.setOnline(user.getUserId());
        return AuthResponse.builder().token(token).authenticated(true).build();
    }

    private String generateToken(UserProfile user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserId())
                .issuer("Blur.vn")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest introspectRequest) throws JOSEException, ParseException {
        var token = introspectRequest.getToken();
        boolean isValid = true;
        SignedJWT signedJWT = null;
        try {
            signedJWT = verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .userId(Objects.nonNull(signedJWT) ? signedJWT.getJWTClaimsSet().getSubject() : null)
                .valid(isValid)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            redisService.invalidateToken(jit, Duration.between(Instant.now(), expiryTime.toInstant()));
            redisService.setOffline(signToken.getJWTClaimsSet().getSubject());
        } catch (AppException e) {
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = verifyToken(request.getToken(), true);
        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();

        redisService.invalidateToken(jit, Duration.between(Instant.now(), expiryTime.toInstant()));

        String userId = signJWT.getJWTClaimsSet().getSubject();

        UserProfile user = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String token = generateToken(user);

        return AuthResponse.builder().token(token).authenticated(true).build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationDate = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier);
        if (!verified && expirationDate.after(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (redisService.isTokenInvalidated(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }

    private String buildScope(UserProfile user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> stringJoiner.add("ROLE_" + role));
        }
        return stringJoiner.toString();
    }

    @Transactional
    public AuthResponse outboundAuthenticationService(String code) {
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URL)
                .grantType(GRANT_TYPE)
                .build());

        var userInfo = outboundUserClient.exchangeToken("json", response.getAccessToken());

        var user = userProfileRepository
                .findByEmail(userInfo.getEmail())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUserId(UUID.randomUUID().toString());
                    profile.setUsername(userInfo.getEmail());
                    profile.setEmail(userInfo.getEmail());
                    profile.setFirstName(userInfo.getGivenName());
                    profile.setLastName(userInfo.getFamilyName());
                    profile.setEmailVerified(true);
                    profile.setRoles(List.of("USER"));
                    profile.setCreatedAt(LocalDate.now());
                    profile.setUpdatedAt(LocalDate.now());
                    return userProfileRepository.save(profile);
                });

        var token = generateToken(user);

        return AuthResponse.builder().token(token).build();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/service/RedisService.java`
```java
package com.blur.userservice.identity.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final String INVALID_TOKEN_PREFIX = "token:invalid:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(30);
    RedisTemplate<String, Object> redisTemplate;

    public void setOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, System.currentTimeMillis(), ONLINE_TTL);
        } catch (RedisConnectionFailureException e) {
        } catch (Exception e) {
        }
    }

    public void setOffline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
        } catch (Exception e) {
        }
    }

    public boolean isOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            return redisTemplate.hasKey(key);
        } catch (RedisConnectionFailureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateToken(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.trim().isEmpty() || ttl == null || ttl.isNegative()) {
            return;
        }
        String key = INVALID_TOKEN_PREFIX + tokenId;
        try {
            redisTemplate.opsForValue().set(key, "1", ttl);
        } catch (RedisConnectionFailureException e) {
        } catch (Exception e) {
        }
    }

    public boolean isTokenInvalidated(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return false;
        }
        String key = INVALID_TOKEN_PREFIX + tokenId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/service/UserService.java`
```java
package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.UserCreationPasswordRequest;
import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.dto.request.UserUpdateRequest;
import com.blur.userservice.identity.dto.response.UserResponse;
import com.blur.userservice.identity.exception.AppException;
import com.blur.userservice.identity.exception.ErrorCode;
import com.blur.userservice.identity.mapper.UserMapper;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserProfileRepository userProfileRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userById", key = "#result.userId", condition = "#result != null")
    })
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userProfileRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (StringUtils.hasText(request.getEmail())
                && userProfileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(UUID.randomUUID().toString());
        profile.setUsername(request.getUsername());
        profile.setEmail(request.getEmail());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDob(request.getDob());
        profile.setCity(request.getCity());
        profile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        profile.setRoles(List.of("USER"));
        profile.setEmailVerified(false);
        profile.setCreatedAt(LocalDate.now());
        profile.setUpdatedAt(LocalDate.now());

        try {
            profile = userProfileRepository.save(profile);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(profile);
    }

    public void createPassword(UserCreationPasswordRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        UserProfile user = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (StringUtils.hasText(user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_EXISTED);
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userProfileRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userProfileRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @Cacheable(value = "userById", key = "#userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserProfile getUserById(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public UserProfile updateUser(String userId, UserUpdateRequest request) {
        UserProfile user = getUserById(userId);
        userMapper.updateUser(user, request);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(LocalDate.now());
        return userProfileRepository.save(user);
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "myInfo", key = "#userId")
    })
    public void deleteUser(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> userProfileRepository.deleteById(profile.getId()));
    }

    @Cacheable(value = "myInfo", key = "#root.target.getCurrentUserId()", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        UserProfile user = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPasswordHash()));
        return userResponse;
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/identity/util/CookieUtil.java`
```java
package com.blur.userservice.identity.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    @Value("${jwt.valid-duration}")
    private long validDuration;
    @Value("${cookie.domain:localhost}")
    private String cookieDomain;
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true) // JavaScript khÃ´ng thá»ƒ truy cáº­p
                .secure(cookieSecure) // true cho production (HTTPS)
                .path("/") // Cookie valid cho táº¥t cáº£ paths
                .maxAge(Duration.ofSeconds(validDuration))
                .sameSite("Lax") // CSRF protection
                .domain(cookieDomain)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token, long refreshDuration) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh") // Chá»‰ gá»­i khi refresh
                .maxAge(Duration.ofSeconds(refreshDuration))
                .sameSite("Strict")
                .domain(cookieDomain)
                .build();
    }

    public ResponseCookie createLogoutCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0) // XÃ³a cookie
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/controller/InternalUserProfileController.java`
```java
package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserProfileController {
  UserProfileService userProfileService;
  UserProfileRepository userProfileRepository;
  UserProfileMapper userProfileMapper;

  @PostMapping("/internal/users")
  public ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest request) {
    var result = userProfileService.createProfile(request);
    result.setCreatedAt(LocalDate.now());
    return ApiResponse.<UserProfileResponse>builder()
        .code(1000)
        .result(result)
        .build();
  }

  @GetMapping("/internal/users/{userId}")
  public ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId) {
    return ApiResponse.<UserProfileResponse>builder()
        .result(userProfileService.getByUserId(userId))
        .build();
  }

  /**
   * lay danh sach userId cua tat ca followers cua mot user
   * dung cho content-service khi publish post_created event(cqrs feed)
   */
  @GetMapping("/internal/user/{userId}/follower-ids")
  public ApiResponse<List<String>> getFollowerIds(@PathVariable String userId) {
    List<String> followerIds = userProfileService.getFollowerUserIds(userId);
    return ApiResponse.<List<String>>builder()
        .code(1000)
        .result(followerIds)
        .build();
  }

  /**
   * Láº¥y táº¥t cáº£ profiles (dÃ¹ng cho test data generation)
   */
  @GetMapping("/internal/users/all")
  public ApiResponse<List<UserProfileResponse>> getAllProfiles() {
    List<UserProfile> profiles = userProfileRepository.findAll();
    List<UserProfileResponse> result = profiles.stream()
        .map(userProfileMapper::toUserProfileResponse)
        .collect(Collectors.toList());
    return ApiResponse.<List<UserProfileResponse>>builder()
        .code(1000)
        .result(result)
        .build();
  }

  /**
   * Follow user internal (khÃ´ng cáº§n authentication - chá»‰ dÃ¹ng cho test)
   */
  @PostMapping("/internal/users/follow")
  public ApiResponse<String> followUserInternal(
      @RequestParam("fromProfileId") String fromProfileId,
      @RequestParam("toProfileId") String toProfileId) {
    try {
      userProfileRepository.follow(fromProfileId, toProfileId);
      return ApiResponse.<String>builder()
          .code(1000)
          .result("Followed successfully")
          .build();
    } catch (Exception e) {
      return ApiResponse.<String>builder()
          .code(5000)
          .message("Failed to follow: " + e.getMessage())
          .build();
    }
  }

  /**
   * ====================================================
   * ENDPOINT CHÃNH: Táº¡o random follows cho táº¥t cáº£ users
   * ====================================================
   * <p>
   * Sá»­ dá»¥ng trong Postman:
   * POST http://localhost:8082/profile/internal/generate-follows?minFollows=5&maxFollows=50
   */
  @PostMapping("/internal/generate-follows")
  public ApiResponse<Map<String, Object>> generateRandomFollows(
      @RequestParam(defaultValue = "5") int minFollows,
      @RequestParam(defaultValue = "50") int maxFollows) {

    long startTime = System.currentTimeMillis();
    Map<String, Object> result = new HashMap<>();


    try {
      // CHá»ˆ láº¥y danh sÃ¡ch IDs - trÃ¡nh load full entity + relationships
      List<String> allProfileIds = userProfileRepository.findAllProfileIds();
      int profileCount = allProfileIds.size();

      if (profileCount < 2) {
        result.put("error", "Need at least 2 profiles to create follows");
        return ApiResponse.<Map<String, Object>>builder()
            .code(4000)
            .message("Not enough profiles")
            .result(result)
            .build();
      }


      Random random = new Random();
      AtomicInteger totalFollows = new AtomicInteger(0);
      AtomicInteger failedFollows = new AtomicInteger(0);

      for (int i = 0; i < profileCount; i++) {
        String fromId = allProfileIds.get(i);
        int followCount = random.nextInt(maxFollows - minFollows + 1) + minFollows;

        // Chá»n ngáº«u nhiÃªn ngÆ°á»i Ä‘á»ƒ follow
        Set<Integer> followedIndices = new HashSet<>();
        while (followedIndices.size() < followCount && followedIndices.size() < profileCount - 1) {
          int targetIndex = random.nextInt(profileCount);
          if (targetIndex != i) {
            followedIndices.add(targetIndex);
          }
        }

        // Táº¡o follow relationships báº±ng Cypher query (chá»‰ dÃ¹ng ID)
        for (int targetIndex : followedIndices) {
          String toId = allProfileIds.get(targetIndex);
          try {
            userProfileRepository.follow(fromId, toId);
            totalFollows.incrementAndGet();
          } catch (Exception e) {
            failedFollows.incrementAndGet();
          }
        }

      }

      // Cáº­p nháº­t follow counts cho táº¥t cáº£ users
      long updatedCount = userProfileRepository.updateAllFollowCounts();

      long duration = System.currentTimeMillis() - startTime;

      result.put("totalProfiles", profileCount);
      result.put("totalFollowsCreated", totalFollows.get());
      result.put("failedFollows", failedFollows.get());
      result.put("followCountsUpdated", updatedCount);
      result.put("durationMs", duration);
      result.put("durationFormatted", formatDuration(duration));
      result.put("avgFollowsPerUser", (double) totalFollows.get() / profileCount);


      return ApiResponse.<Map<String, Object>>builder()
          .code(1000)
          .message("Random follows generated successfully!")
          .result(result)
          .build();

    } catch (Exception e) {
      result.put("error", e.getMessage());
      return ApiResponse.<Map<String, Object>>builder()
          .code(5000)
          .message("Error: " + e.getMessage())
          .result(result)
          .build();
    }
  }

  /**
   * Set random city cho táº¥t cáº£ users
   * <p>
   * POST http://localhost:8082/profile/internal/generate-cities
   */
  @PostMapping("/internal/generate-cities")
  public ApiResponse<Map<String, Object>> generateRandomCities() {
    long startTime = System.currentTimeMillis();
    Map<String, Object> result = new HashMap<>();

    String[] CITIES = {
        "HÃ  Ná»™i", "Há»“ ChÃ­ Minh", "ÄÃ  Náºµng", "Háº£i PhÃ²ng", "Cáº§n ThÆ¡",
        "BiÃªn HÃ²a", "Nha Trang", "Huáº¿", "BuÃ´n Ma Thuá»™t", "Quy NhÆ¡n",
        "VÅ©ng TÃ u", "ÄÃ  Láº¡t", "Long XuyÃªn", "ThÃ¡i NguyÃªn", "Nam Äá»‹nh",
        "Vinh", "Thanh HÃ³a", "Báº¯c Ninh"
    };

    try {
      // CHá»ˆ láº¥y IDs - trÃ¡nh load full entity
      List<String> allProfileIds = userProfileRepository.findAllProfileIds();
      Random random = new Random();
      int updated = 0;

      for (String profileId : allProfileIds) {
        // DÃ¹ng Cypher query trá»±c tiáº¿p, KHÃ”NG load entity
        userProfileRepository.setCity(profileId, CITIES[random.nextInt(CITIES.length)]);
        updated++;

        if (updated % 500 == 0) {
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      result.put("totalUpdated", updated);
      result.put("durationMs", duration);
      result.put("durationFormatted", formatDuration(duration));

      return ApiResponse.<Map<String, Object>>builder()
          .code(1000)
          .message("Random cities assigned successfully!")
          .result(result)
          .build();

    } catch (Exception e) {
      result.put("error", e.getMessage());
      return ApiResponse.<Map<String, Object>>builder()
          .code(5000)
          .message("Error: " + e.getMessage())
          .result(result)
          .build();
    }
  }

  private String formatDuration(long ms) {
    long seconds = ms / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;
    if (minutes > 0) {
      return String.format("%d min %d sec", minutes, seconds);
    }
    return String.format("%d sec %d ms", seconds, ms % 1000);
  }
}

```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/controller/RecommendationController.java`
```java
package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.response.RecommendationPageResponse;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {

    UserProfileService userProfileService;


    // goi t dua tren ket noi chung
    @GetMapping("/mutual")
    public ApiResponse<RecommendationPageResponse> getMutualRecommendatApiResponse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getMutualRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder()
                .result(result)
                .build();
    }

    // goi y cung thanh pho
    @GetMapping("/nearby")
    public ApiResponse<RecommendationPageResponse> getSameCityRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getSameCityRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder()
                .result(result)
                .build();
    }

    // goi y dua tren so thich tuong tu
    @GetMapping("/similar")
    public ApiResponse<RecommendationPageResponse> getMimilarRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        RecommendationPageResponse result = userProfileService.getSimilarTasteRecommendations(page, size);
        return ApiResponse.<RecommendationPageResponse>builder()
                .result(result)
                .build();
    }

    // goi y nguoi dung pho bien
    @GetMapping("/popular")
    public ApiResponse<RecommendationPageResponse> getPopularRecommendations(
            @RequestParam(defaultValue = "100") int minFollowers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = userProfileService.getPopularRecommendations(minFollowers, page, size);
        return ApiResponse.<RecommendationPageResponse>builder()
                .result(result)
                .build();
    }
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/controller/UserProfileController.java`
```java
package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.UserProfileUpdateRequest;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {
    UserProfileService userProfileService;
    UserProfileMapper userProfileMapper;

    @GetMapping("/users/{profileId}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable String profileId) {
        var result = userProfileMapper.toUserProfileResponse(userProfileService.getUserProfile(profileId));
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(result)
                .build();
    }

    @GetMapping("/users/")
    public ApiResponse<List<UserProfileResponse>> getUserProfiles() {
        var result = userProfileService.getAllUserProfiles();
        return ApiResponse.<List<UserProfileResponse>>builder()
                .code(1000)
                .result(result)
                .build();
    }

    @PutMapping("/users/{userProfileId}")
    public ApiResponse<UserProfileResponse> updateUserProfile(@PathVariable String userProfileId, @RequestBody UserProfileUpdateRequest request) {
        var profileUpdated = userProfileService.updateUserProfile(userProfileId, request);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(profileUpdated)
                .build();
    }

    @DeleteMapping("/users/{userProfileId}")
    public ApiResponse<String> deleteUserProfile(@PathVariable String userProfileId) {
        userProfileService.deleteUserProfile(userProfileId);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("User Profile has been deleted")
                .build();
    }

    @GetMapping("/users/myInfo")
    public ApiResponse<UserProfileResponse> myInfo() {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.myProfile())
                .build();
    }

    @PutMapping("/users/follow/{userId}")
    public ApiResponse<String> followUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(userProfileService.followUser(userId))
                .build();
    }

    @PutMapping("/users/unfollow/{userId}")
    public ApiResponse<String> unfollowUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(userProfileService.unfollowUser(userId))
                .build();
    }

    @GetMapping("/users/search/{firstName}")
    public ApiResponse<List<UserProfileResponse>> searchUserProfiles(@PathVariable String firstName) {
        var result = userProfileService.findUserProfileByFirstName(firstName);
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/users/follower/{profileId}")
    public ApiResponse<List<UserProfileResponse>> followers(@PathVariable String profileId) {
        var result = userProfileService.getFollowers(profileId);

        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/users/following/{profileId}")
    public ApiResponse<List<UserProfileResponse>> followings(@PathVariable String profileId) {
        var result = userProfileService.getFollowing(profileId);
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @PostMapping("/users/search")
    ApiResponse<List<UserProfileResponse>> search(@RequestParam(value = "name") String request) {
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(userProfileService.search(request))
                .build();
    }

}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/dto/ApiResponse.java`
```java
package com.blur.userservice.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000;

    private String message;
    private T result;
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/entity/UserProfile.java`
```java
package com.blur.userservice.profile.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("user_profile")// khai bao entity trong neo4j
public class UserProfile {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    @Property("user_id") // tuong nhu nhu column ben dbms khac
    String userId;
    String username;
    String passwordHash;
    String firstName;
    String lastName;
    String bio;
    String city;
    String phone;
    String email;
    String gender;
    String website;
    String imageUrl;
    String address;
    LocalDate updatedAt;
    LocalDate dob;
    LocalDate createdAt;
    LocalDate lastActiveAt;
    Integer followersCount;
    Integer followingCount;
    Integer postCount;
    Boolean verified;
    Boolean emailVerified;
    List<String> roles;

    @JsonIgnore
    @Relationship(type = "follows", direction = Relationship.Direction.OUTGOING)
    Set<UserProfile> following = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "follows", direction = Relationship.Direction.INCOMING)
    Set<UserProfile> followers = new HashSet<>();
}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/repository/UserProfileRepository.java`
```java
package com.blur.userservice.profile.repository;

import com.blur.userservice.profile.entity.UserProfile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {

  // Override default methods to prevent infinite recursion on self-referencing @Relationship
  @Query("MATCH (u:user_profile {user_id: $userId}) RETURN u")
  Optional<UserProfile> findUserProfileByUserId(String userId);

  @Query("MATCH (u:user_profile) WHERE u.id = $id RETURN u")
  Optional<UserProfile> findUserProfileById(String id);

  @Query("MATCH (u:user_profile {user_id: $userId}) RETURN u")
  Optional<UserProfile> findByUserId(String userId);

  @Query("MATCH (u:user_profile) WHERE toLower(u.username) = toLower($username) RETURN u")
  Optional<UserProfile> findByUsername(String username);

  @Query("MATCH (u:user_profile) WHERE toLower(u.email) = toLower($email) RETURN u")
  Optional<UserProfile> findByEmail(String email);

  @Query("MATCH (u:user_profile)-[:follows]->(f:user_profile) WHERE u.id = $id RETURN f")
  List<UserProfile> findAllFollowingById(String id);

  @Query("MATCH (f:user_profile)-[:follows]->(u:user_profile) WHERE u.id = $id RETURN f")
  List<UserProfile> findAllFollowersById(@Param("id") String id);


  @Query("MATCH (u:user_profile) WHERE toLower(u.firstName) CONTAINS toLower($firstName) RETURN u")
  List<UserProfile> findAllByFirstNameContainingIgnoreCase(String firstName);

  @Query("""
      MATCH (a:user_profile {id: $fromId})
      MATCH (b:user_profile {id: $toId})
      MERGE (a)-[:follows]->(b)
      """)
  void follow(@Param("fromId") String fromId, @Param("toId") String toId);

  @Query("""
      MATCH (a:user_profile {id: $fromId})-[r:follows]->(b:user_profile {id: $toId})
      DELETE r
      """)
  void unfollow(@Param("fromId") String fromId, @Param("toId") String toId);

  @Query("MATCH (u:user_profile) WHERE u.username CONTAINS $username RETURN u")
  List<UserProfile> findAllByUsernameLike(String username);

  // follower chung (ban cua ban be)
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
          MATCH (myFollowing)-[:follows]->(recommended:user_profile)
          WHERE recommended <> me 
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
            AND NOT EXISTS((recommended)-[:BLOCKED]->(me))
          WITH recommended, COUNT(DISTINCT myFollowing) AS mutualCount
          ORDER BY mutualCount DESC
          SKIP $skip
          LIMIT $limit
          RETURN recommended
      """)
  List<UserProfile> findMutualRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit
  );

  // dem so goi y dua tren ket noi chung
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
          SKIP $skip
          LIMIT $limit
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
      @Param("limit") int limit
  );


  // tim nguoi co so thich tuong tu (follow cung nguoi)
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          MATCH (recommended:user_profile)-[:follows]->(following)
          WHERE recommended <> me 
            AND NOT (me)-[:follows]->(recommended)
            AND NOT (recommended)-[:follows]->(me)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          WITH recommended, COUNT(DISTINCT following) AS sharedCount
          ORDER BY sharedCount DESC, recommended.followersCount DESC
          SKIP $skip
          LIMIT $limit
          RETURN recommended
      """)
  List<UserProfile> findSimilarTasteRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit
  );

  // dem so goi y dua tren so thich tuong tu
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          MATCH (recommended:user_profile)-[:follows]->(following)
          WHERE recommended <> me 
            AND NOT (me)-[:follows]->(recommended)
            AND NOT (recommended)-[:follows]->(me)
          RETURN COUNT(DISTINCT recommended)
      """)
  long countSimilarTasteRecommendations(@Param("userId") String userId);


  // tim nguoi co thuoc tinh chung (chung thanh pho)
  @Query("""
          MATCH (me:user_profile {id: $userId})
          WHERE me.city IS NOT NULL AND me.city <> ''
          MATCH (recommended:user_profile)
          WHERE recommended.city = me.city
            AND recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          RETURN recommended
          ORDER BY recommended.followersCount DESC, recommended.updatedAt DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findSameCityRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit
  );

  /**
   * Äáº¿m tá»•ng sá»‘ ngÆ°á»i dÃ¹ng cÃ¹ng thÃ nh phá»‘
   */
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

  /**
   * TÃ¬m ngÆ°á»i dÃ¹ng phá»• biáº¿n vÃ  hoáº¡t Ä‘á»™ng gáº§n Ä‘Ã¢y
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
          MATCH (recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
            AND recommended.followersCount >= $minFollowers
          RETURN recommended
          ORDER BY recommended.followersCount DESC, recommended.updatedAt DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findPopularRecommendations(
      @Param("userId") String userId,
      @Param("minFollowers") int minFollowers,
      @Param("skip") int skip,
      @Param("limit") int limit
  );

  /**
   * Äáº¿m ngÆ°á»i dÃ¹ng phá»• biáº¿n
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
          MATCH (recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND recommended.followersCount >= $minFollowers
          RETURN COUNT(DISTINCT recommended)
      """)
  long countPopularRecommendations(
      @Param("userId") String userId,
      @Param("minFollowers") int minFollowers
  );

  /**
   * Gá»£i Ã½ káº¿t há»£p Ä‘a yáº¿u tá»‘ vá»›i trá»ng sá»‘
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
      
          // Yáº¿u tá»‘ 1: Káº¿t ná»‘i chung (weight = 3)
          OPTIONAL MATCH (me)-[:follows]->(myFollowing:user_profile)-[:follows]->(mutualRec:user_profile)
          WHERE mutualRec <> me 
            AND NOT (me)-[:follows]->(mutualRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(mutualRec))
      
          // Yáº¿u tá»‘ 2: CÃ¹ng thÃ nh phá»‘ (weight = 2)
          OPTIONAL MATCH (cityRec:user_profile)
          WHERE cityRec.city = me.city 
            AND cityRec <> me 
            AND NOT (me)-[:follows]->(cityRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(cityRec))
      
          // Yáº¿u tá»‘ 3: Sá»Ÿ thÃ­ch tÆ°Æ¡ng tá»± (weight = 2)
          OPTIONAL MATCH (me)-[:follows]->(following:user_profile)<-[:follows]-(tasteRec:user_profile)
          WHERE tasteRec <> me 
            AND NOT (me)-[:follows]->(tasteRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(tasteRec))
      
          WITH me, 
               COLLECT(DISTINCT mutualRec) AS mutualRecs,
               COLLECT(DISTINCT cityRec) AS cityRecs,
               COLLECT(DISTINCT tasteRec) AS tasteRecs
      
          // Káº¿t há»£p táº¥t cáº£
          WITH mutualRecs + cityRecs + tasteRecs AS allRecs, mutualRecs, cityRecs, tasteRecs
          UNWIND allRecs AS rec
          WHERE rec IS NOT NULL
      
          // TÃ­nh Ä‘iá»ƒm
          WITH rec, 
               CASE WHEN rec IN mutualRecs THEN 3 ELSE 0 END AS mutualScore,
               CASE WHEN rec IN cityRecs THEN 2 ELSE 0 END AS cityScore,
               CASE WHEN rec IN tasteRecs THEN 2 ELSE 0 END AS tasteScore
      
          WITH rec, (mutualScore + cityScore + tasteScore) AS totalScore
      
          RETURN DISTINCT rec
          ORDER BY totalScore DESC, rec.followersCount DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findCombinedRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit
  );

  // ============================================
  // 5.6 KIá»‚M TRA ÄÃƒ FOLLOW
  // ============================================

  /**
   * Kiá»ƒm tra xem user A Ä‘Ã£ follow user B chÆ°a
   */
  @Query("""
          MATCH (a:user_profile {id: $fromId})-[r:follows]->(b:user_profile {id: $toId})
          RETURN COUNT(r) > 0
      """)
  boolean isFollowing(@Param("fromId") String fromId, @Param("toId") String toId);

  /**
   * Láº¥y danh sÃ¡ch IDs cá»§a nhá»¯ng ngÆ°á»i mÃ  user Ä‘ang follow
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          RETURN following.id
      """)
  List<String> findFollowingIds(@Param("userId") String userId);

  // ============================================
  // Cáº¬P NHáº¬T Sá» Äáº¾M DENORMALIZED
  // ============================================

  /**
   * Cáº­p nháº­t sá»‘ followers vÃ  following count
   */
  @Query("""
          MATCH (u:user_profile {id: $userId})
          OPTIONAL MATCH (follower:user_profile)-[:follows]->(u)
          OPTIONAL MATCH (u)-[:follows]->(following:user_profile)
          WITH u, COUNT(DISTINCT follower) AS followers, COUNT(DISTINCT following) AS following
          SET u.followersCount = followers, u.followingCount = following
          RETURN u
      """)
  UserProfile updateFollowCounts(@Param("userId") String userId);

  /**
   * Cáº­p nháº­t táº¥t cáº£ follow counts (cho migration/batch job)
   */
  @Query("""
          MATCH (u:user_profile)
          OPTIONAL MATCH (follower:user_profile)-[:follows]->(u)
          OPTIONAL MATCH (u)-[:follows]->(following:user_profile)
          WITH u, COUNT(DISTINCT follower) AS followers, COUNT(DISTINCT following) AS following
          SET u.followersCount = followers, u.followingCount = following
          RETURN COUNT(u)
      """)
  long updateAllFollowCounts();

  /**
   * Láº¥y chá»‰ danh sÃ¡ch IDs cá»§a táº¥t cáº£ profiles (trÃ¡nh load full entity)
   */
  @Query("MATCH (u:user_profile) RETURN u.id")
  List<String> findAllProfileIds();

  /**
   * Set city cho 1 user (dÃ¹ng Cypher, khÃ´ng cáº§n load entity)
   */
  @Query("MATCH (u:user_profile {id: $profileId}) SET u.city = $city")
  void setCity(@Param("profileId") String profileId, @Param("city") String city);

  /**
   * Äáº¿m tá»•ng sá»‘ profiles
   */
  @Query("MATCH (u:user_profile) RETURN COUNT(u)")
  long countProfiles();

  /**
   * lay danh sach userId cua nhung nguoi dang follow 1 user (theo userId, khong phai profileId
   * dung cqrs cho feed: khi tao post -> lay followe userId -> tao feed item cho moi follower
   */
  @Query("""
      MATCH(follower:user_profile)-[:follows]->(u:user_profile {user_id: $userId})
      RETURN follower.user_id
      """)
  List<String> findFollowerUserIdsByUserId(@Param("userId") String userId);

  
  interface MutualRecommendationProjection {
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

}
```

## `Backend/user-service/src/main/java/com/blur/userservice/profile/service/UserProfileService.java`
```java
package com.blur.userservice.profile.service;

import com.blur.userservice.profile.dto.event.Event;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.dto.request.UserProfileUpdateRequest;
import com.blur.userservice.profile.dto.response.RecommendationPageResponse;
import com.blur.userservice.profile.dto.response.RecommendationResponse;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.enums.RecommendationType;
import com.blur.userservice.profile.exception.AppException;
import com.blur.userservice.profile.exception.ErrorCode;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.repository.UserProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {
  UserProfileRepository userProfileRepository;
  UserProfileMapper userProfileMapper;


  @Caching(
      evict = {
          @CacheEvict(value = "profileByUserId", key = "#request.userId")
      }
  )
  public UserProfileResponse createProfile(ProfileCreationRequest request) {
    UserProfile userProfile = userProfileMapper.toUserProfile(request);
    userProfile.setUserId(request.getUserId());
    userProfile.setUsername(request.getUsername());
    userProfile.setCreatedAt(LocalDate.now());
    userProfile.setEmail(request.getEmail());
    try {
      userProfile = userProfileRepository.save(userProfile);
    } catch (DataIntegrityViolationException ex) {
      throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
    }
    return userProfileMapper.toUserProfileResponse(userProfile);
  }

  public UserProfile getUserProfile(String id) {
    return userProfileRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
  }

  @Cacheable(
      value = "searchResults",
      key = "'firstName-' + #firstName",
      unless = "#result == null || #result.isEmpty()"
  )
  public List<UserProfileResponse> findUserProfileByFirstName(String firstName) {
    return userProfileRepository.findAllByFirstNameContainingIgnoreCase(firstName)
        .stream()
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Cacheable(
      value = "profiles",
      key = "'all'",
      unless = "#result == null || #result.isEmpty()"
  )
  public List<UserProfileResponse> getAllUserProfiles() {
    return userProfileRepository.findAll()
        .stream()
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }

  @Cacheable(value = "profileByUserId", key = "#userId", unless = "#result == null")
  public UserProfileResponse getByUserId(String userId) {
    return userProfileMapper.toUserProfileResponse(getOrCreateProfileByUserId(userId));
  }

  @Cacheable(
      value = "myProfile",
      key = "#root.target.getCurrentUserId()",
      unless = "#result == null "
  )
  public UserProfileResponse myProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();
    return userProfileMapper.toUserProfileResponse(getOrCreateProfileByUserId(userId));
  }

  @Caching(
      evict = {
          @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
          @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
          @CacheEvict(value = "searchResults", allEntries = true)
      }
  )
  public UserProfileResponse updateUserProfile(String userProfileId, UserProfileUpdateRequest request) {
    UserProfile userProfile = getUserProfile(userProfileId);
    userProfileMapper.updateUserProfile(userProfile, request);
    UserProfile saved = userProfileRepository.save(userProfile);
    return userProfileMapper.toUserProfileResponse(saved);
  }

  @Caching(evict = {
      @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
      @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
      @CacheEvict(value = "followers", allEntries = true),
      @CacheEvict(value = "following", allEntries = true),
      @CacheEvict(value = "searchResults", allEntries = true)
  })
  public void deleteUserProfile(String userProfileId) {
    userProfileRepository.deleteById(userProfileId);
  }

  @Caching(evict = {
      @CacheEvict(value = "followers", key = "#followerId"),
      @CacheEvict(value = "following", key = "#root.target.getCurrentUserId()")
  })
  public String followUser(String followerId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String reqUserId = authentication.getName();

    if (reqUserId.equals(followerId)) {
      throw new AppException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
    }

    // Láº¥y Neo4j UUID tá»« userId
    var requester = getOrCreateProfileByUserId(reqUserId);

    var followingUser = userProfileRepository.findUserProfileById(followerId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    userProfileRepository.follow(requester.getId(), followerId);

    // gui notification
    Event event = Event.builder()
        .senderId(requester.getId())
        .senderName(requester.getFirstName() + " " + requester.getLastName())
        .receiverId(followingUser.getId())
        .receiverName(followingUser.getFirstName() + " " + followingUser.getLastName())
        .receiverEmail(followingUser.getEmail())
        .timestamp(LocalDateTime.now())
        .build();


    return "You are following " + followingUser.getFirstName();
  }

  @Caching(evict = {
      @CacheEvict(value = "followers", key = "#followerId"),
      @CacheEvict(value = "following", key = "#root.target.getCurrentUserId()")
  })
  public String unfollowUser(String followerId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String reqUserId = authentication.getName();

    var requester = getOrCreateProfileByUserId(reqUserId);

    var followingUser = userProfileRepository.findUserProfileById(followerId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
    requester.getFollowers().remove(followingUser);
    userProfileRepository.unfollow(requester.getId(), followerId);

    return "You unfollowed " + followingUser.getFirstName();
  }

  @Cacheable(
      value = "followers",
      key = "#profileId",
      unless = "#result == null || #result.isEmpty()"
  )
  public List<UserProfileResponse> getFollowers(String profileId) {
    return userProfileRepository.findAllFollowersById(profileId)
        .stream()
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }

  @Cacheable(
      value = "following",
      key = "#profileId",
      unless = "#result == null || #result.isEmpty()"
  )
  public List<UserProfileResponse> getFollowing(String profileId) {
    return userProfileRepository.findAllFollowingById(profileId)
        .stream()
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }


  @Cacheable(
      value = "searchResults",
      key = "'username-' + #request",
      unless = "#result == null || #result.isEmpty()"
  )
  public List<UserProfileResponse> search(String request) {
    var userId = SecurityContextHolder.getContext().getAuthentication().getName();
    List<UserProfile> userProfiles = userProfileRepository.findAllByUsernameLike(request);
    return userProfiles.stream()
        .filter(userProfile -> !userId.equals(userProfile.getUserId()))
        .map(userProfileMapper::toUserProfileResponse)
        .toList();
  }


  // lay goi y dua tren ket noi chung
  // cache 10 phut
  @Cacheable(
      value = "recommendation:mutual",
      key = "#root.target.getCurrentUserProfileId",
      unless = "#result.content.isEmpty()"
  )
  @Transactional(readOnly = true)
  public RecommendationPageResponse getMutualRecommendations(int page, int size) {
    String userId = getCurrentProfileId();
    int skip = page * size;
    List<UserProfile> recommendations = userProfileRepository.findMutualRecommendations(userId, skip, size);
    long total = userProfileRepository.countMutualRecommendations(userId);
    List<RecommendationResponse> content = recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.MUTUAL))
        .collect(Collectors.toList());
    return buildPageResponse(content, page, size, total);
  }

  // lay goi y dua tren so thich tuong tu (follow cung nguoi)
  @Cacheable(
      value = "recommendations:taste",
      key = "#root.tartget.getCurrentProfileId() +'-'+#page+'-'+#size",
      unless = "#result.content.isEmpty()"
  )
  @Transactional(readOnly = true)
  public RecommendationPageResponse getSimilarTasteRecommendations(int page, int size) {
    String userId = getCurrentProfileId();
    int skip = page * size;
    List<UserProfile> recommendations = userProfileRepository
        .findSimilarTasteRecommendations(userId, skip, size);
    long total = userProfileRepository.countSimilarTasteRecommendations(userId);
    List<RecommendationResponse> content = recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.SAME_CITY))
        .collect(Collectors.toList());
    return buildPageResponse(content, page, size, total);
  }

  // lay goi y nguoi dung cung thanh pho
  @Cacheable(
      value = "recommnedations:city",
      key = "#root.target.getCurrentProfileId() + '-' + #page + '-' + #size",
      unless = "#result.content.isEmpty()"
  )
  @Transactional(readOnly = true)
  public RecommendationPageResponse getSameCityRecommendations(int page, int size) {
    String userId = getCurrentProfileId();
    int skip = page * size;
    List<UserProfile> recommendations = userProfileRepository
        .findSameCityRecommendations(userId, skip, size);
    long total = userProfileRepository.countSameCityRecommendations(userId);
    List<RecommendationResponse> content = recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.SAME_CITY))
        .collect(Collectors.toList());
    return buildPageResponse(content, page, size, total);
  }

  // lay goi y nguoi dung pho bien (nhieu follower)
  @Cacheable(
      value = "recommedations:popular",
      key = "#root.target.getCurrentProfileId() +'-'+ #minFollowers +'-' + #page+'-'+#size",
      unless = "#result.content.isEmpty()"
  )
  @Transactional(readOnly = true)
  public RecommendationPageResponse getPopularRecommendations(int minFollowers, int page, int size) {
    String userId = getCurrentProfileId();
    int skip = page * size;
    List<UserProfile> recommendations = userProfileRepository
        .findPopularRecommendations(userId, minFollowers, skip, size);
    long total = userProfileRepository.countPopularRecommendations(userId, minFollowers);
    List<RecommendationResponse> content = recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.POPULAR))
        .collect(Collectors.toList());
    return buildPageResponse(content, page, size, total);
  }

  // lay goi y da yeu to
  // uu tien ket noi chung > cung thanh pho > so thich > pho bien
  @Cacheable(
      value = "recommendations:combined",
      key = "#root.target.getCurrentProfileId() + '-' + #limit",
      unless = "#result.isEmpty()"
  )
  @Transactional(readOnly = true)
  public List<RecommendationResponse> getCombinedRecommendations(int limit) {
    String userId = getCurrentProfileId();
    // su dung set de giu thu tu va loai bo trung lap
    Set<String> addedIds = new HashSet<>();
    List<RecommendationResponse> combined = new ArrayList<>();
    // uu tien cao nhat: ket noi chung (40%
    int mutualLimit = (int) Math.ceil(limit * 0.4);
    List<UserProfile> mutualRecs = userProfileRepository.findMutualRecommendations(userId, 0, mutualLimit);
    for (UserProfile profile : mutualRecs) {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.MUTUAL));
      }
    }
    // 2 cung thanh pho (25%)
    int cityLimit = (int) Math.ceil(limit * 0.25);
    List<UserProfile> cityRecs = userProfileRepository
        .findSameCityRecommendations(userId, 0, cityLimit);
    cityRecs.forEach(profile -> {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.SAME_CITY));
      }
    });
    // so thich tuong tu (25%)
    int tasteLimit = (int) Math.ceil(limit * 0.25);
    List<UserProfile> tasteRecs = userProfileRepository
        .findSimilarTasteRecommendations(userId, 0, tasteLimit);
    tasteRecs.forEach(profile -> {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.SIMILAR_TASTE));
      }
    });
    // nguoi dung pho bien (10%
    int popularLimit = (int) Math.ceil(limit * 0.1);
    List<UserProfile> popularRecs = userProfileRepository
        .findPopularRecommendations(userId, 100, 0, popularLimit);
    popularRecs.forEach(profile -> {
      if (addedIds.add(profile.getId())) {
        combined.add(mapToResponse(profile, RecommendationType.POPULAR));
      }
    });
    return combined.stream()
        .limit(limit)
        .collect(Collectors.toList());
  }

  // lay goi y nhanh cho sidebar
  @Cacheable(
      value = "recommendations:quick",
      key = "#root.target.getCurrentProfileId() + '-' + #limit",
      unless = "#result.isEmpty()"
  )
  @Transactional(readOnly = true)
  public List<RecommendationResponse> getQuickRecommendations(int limit) {
    String userId = getCurrentProfileId();
    List<UserProfile> recommendations = userProfileRepository.findCombinedRecommendations(userId, 0, limit);
    return recommendations.stream()
        .map(profile -> mapToResponse(profile, RecommendationType.COMBINED))
        .collect(Collectors.toList());
  }

  // kiem tra da follow chua
  @Transactional(readOnly = true)
  public boolean isFollowing(String targetUserId) {
    String currentUserId = getCurrentProfileId();
    return userProfileRepository.isFollowing(currentUserId, targetUserId);
  }

  // lay danh sach userId cua tat ca followers (dung cho cqrs feed)
  public List<String> getFollowerUserIds(String userId) {
    return userProfileRepository.findFollowerUserIdsByUserId(userId);
  }

  //Vo hieu hoa cache khi co thay doi follow
  @CacheEvict(
      value = {
          "recommendations:mutual",
          "recommendations:taste",
          "recommnedation:city",
          "recommendations:popular",
          "recommendations:combined",
          "recommendations:quick",
      },
      allEntries = true
  )
  public void invalidateRecommnedationCache() {
  }

  // cap nhat follow counts
  @Transactional
  public void updateFollowCounts(String userId) {
    userProfileRepository.updateAllFollowCounts();
  }

  // backfill tat ca follow counts
  @Transactional
  public long backFillAllFollowCounts() {
    long updated = userProfileRepository.updateAllFollowCounts();
    return updated;
  }

  public String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  public String getUserIdByProfileId(String profileId) {
    return userProfileRepository.findById(profileId)
        .map(UserProfile::getUserId)
        .orElse(null);
  }

  /**
   * Láº¥y profile ID cá»§a user hiá»‡n táº¡i tá»« SecurityContext
   */
  public String getCurrentProfileId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    String userId = authentication.getName();
    return getOrCreateProfileByUserId(userId).getId();
  }

  private UserProfile getOrCreateProfileByUserId(String userId) {
    return userProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
  }

  /**
   * Map UserProfile entity sang RecommendationResponse DTO
   */
  private RecommendationResponse mapToResponse(UserProfile profile, RecommendationType type) {
    return RecommendationResponse.builder()
        .id(profile.getId())
        .userId(profile.getUserId())
        .username(profile.getUsername())
        .firstName(profile.getFirstName())
        .lastName(profile.getLastName())
        .imageUrl(profile.getImageUrl())
        .bio(profile.getBio())
        .city(profile.getCity())
        .followerCount(profile.getFollowersCount() != null ? profile.getFollowersCount() : 0)
        .followingCount(profile.getFollowingCount() != null ? profile.getFollowingCount() : 0)
        .mutualConnections(0) // Sáº½ Ä‘Æ°á»£c tÃ­nh riÃªng náº¿u cáº§n
        .recommendationType(type)
        .build();
  }

  /**
   * Build response phÃ¢n trang
   */
  private RecommendationPageResponse buildPageResponse(
      List<RecommendationResponse> content,
      int page,
      int size,
      long total
  ) {
    int totalPages = (int) Math.ceil((double) total / size);

    return RecommendationPageResponse.builder()
        .content(content)
        .page(page)
        .size(size)
        .totalElements(total)
        .totalPages(totalPages)
        .hasNext(page < totalPages - 1)
        .hasPrevious(page > 0)
        .build();
  }
}
```

## `Backend/user-service/src/main/resources/application.yaml`
```yaml
server:
  port: ${SERVER_PORT:8081}
  address: 0.0.0.0
  servlet:
    context-path: /

spring:
  application:
    name: ${SPRING_APPLICATION_NAME:user-service}
  neo4j:
    uri: ${NEO4J_URI}
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD}

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      database: 2
      timeout: ${REDIS_TIME_OUT}
      lettuce:
        pool:
          max-active: ${MAX_ACTIVE}
          max-idle: ${MAX_IDLE}
          min-idle: ${MIN_IDLE}

  cache:
    type: redis

outbound:
  identity:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-url: ${REDIRECT_URL}
    grant-type: authorization_code
    authUri: ${AUTH_URI}

jwt:
  signerKey: ${SIGNER_KEY}
  valid-duration: ${VALID_DURATION}
  refreshable-duration: ${REFRESHABLE_DURATION}

app:
  services:
    profile: ${PROFILE_SERVICE_URL:http://localhost:8081}
    notification: ${NOTIFICATION_SERVICE_URL:http://localhost:8083}

cookie:
  domain: ${COOKIE_DOMAIN:localhost}
  secure: ${COOKIE_SECURE:false}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.springframework.data.neo4j.cypher.deprecation: ERROR
```

