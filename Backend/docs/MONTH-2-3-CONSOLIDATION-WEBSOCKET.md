# 📅 THÁNG 2-3: SERVICE CONSOLIDATION + WEBSOCKET

---

# THÁNG 2: SERVICE CONSOLIDATION

## Tuần 5-6: USER SERVICE (Identity + Profile)

### Task 2.1: Tạo Project Structure
```bash
mkdir -p Backend/user-service/src/main/java/com/blur/user/{identity,profile,config}
mkdir -p Backend/user-service/src/main/resources
```

### Task 2.2: pom.xml
📁 **File:** `Backend/user-service/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.blur</groupId>
    <artifactId>user-service</artifactId>
    <version>1.0.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- MySQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        
        <!-- Neo4j -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-neo4j</artifactId>
        </dependency>
        
        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        
        <!-- Common Lib -->
        <dependency>
            <groupId>com.blur</groupId>
            <artifactId>blur-common-lib</artifactId>
            <version>1.0.0</version>
        </dependency>
        
        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Task 2.3: Multi-Datasource Config
📁 **File:** `user-service/.../config/DataSourceConfig.java`
```java
package com.blur.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

📁 **File:** `user-service/src/main/resources/application.yml`
```yaml
spring:
  application:
    name: user-service
    
  datasource:
    mysql:
      url: jdbc:mysql://localhost:3306/blur_identity
      username: root
      password: ${MYSQL_PASSWORD}
      driver-class-name: com.mysql.cj.jdbc.Driver
      
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD}
      
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  kafka:
    bootstrap-servers: localhost:9092
    
server:
  port: 8080
```

### Task 2.4: Copy Identity Controllers
📁 **File:** `user-service/.../identity/controller/AuthController.java`
```java
package com.blur.user.identity.controller;

// Copy từ IdentityService/controller/AuthController.java
// Giữ nguyên logic, chỉ đổi package

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authService;

    @PostMapping("/token")
    public ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
            .result(authService.authenticate(request))
            .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        return ApiResponse.<IntrospectResponse>builder()
            .result(authService.introspect(request))
            .build();
    }
    
    // ... other methods
}
```

### Task 2.5: Copy Profile Controllers
📁 **File:** `user-service/.../profile/controller/ProfileController.java`
```java
package com.blur.user.profile.controller;

// Copy từ profile-service/controller/UserProfileController.java

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserProfileService profileService;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @GetMapping("/users/{profileId}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable String profileId) {
        return ApiResponse.<UserProfileResponse>builder()
            .result(profileService.getProfile(profileId))
            .build();
    }

    @PutMapping("/users/follow/{userId}")
    public ApiResponse<String> followUser(@PathVariable String userId) {
        String result = profileService.followUser(userId);
        
        // NEW: Publish event
        UserFollowedEvent event = new UserFollowedEvent(getCurrentUserId(), userId);
        outboxRepo.save(OutboxEvent.create("user.followed", userId, 
            objectMapper.writeValueAsString(event)));
        
        return ApiResponse.<String>builder().result(result).build();
    }
    
    // ... other methods
}
```

---

## Tuần 7-8: CONTENT SERVICE (Post + Story)

### Task 2.6: Content Service Structure
```bash
mkdir -p Backend/content-service/src/main/java/com/blur/content/{post,story,comment,config}
```

### Task 2.7: application.yml
📁 **File:** `content-service/src/main/resources/application.yml`
```yaml
spring:
  application:
    name: content-service
    
  data:
    mongodb:
      uri: mongodb://localhost:27017/blur_content
      
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: content-service
      
server:
  port: 8081
```

### Task 2.8: PostService với Outbox
📁 **File:** `content-service/.../post/service/PostService.java`
```java
package com.blur.content.post.service;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public PostResponse createPost(PostRequest request, String userId) {
        Post post = Post.builder()
            .content(request.getContent())
            .userId(userId)
            .createdAt(Instant.now())
            .build();
        post = postRepo.save(post);

        // Publish event for real-time feed
        PostCreatedEvent event = PostCreatedEvent.builder()
            .postId(post.getId())
            .authorId(userId)
            .content(post.getContent())
            .build();
        event.initDefaults();
        
        outboxRepo.save(OutboxEvent.create("post.created", post.getId(),
            objectMapper.writeValueAsString(event)));

        return PostResponse.from(post);
    }

    @Transactional
    public String likePost(String postId, String userId) {
        Post post = postRepo.findById(postId).orElseThrow();
        post.getLikes().add(userId);
        postRepo.save(post);

        // Publish event
        PostLikedEvent event = new PostLikedEvent(postId, userId, post.getUserId());
        outboxRepo.save(OutboxEvent.create("post.liked", postId,
            objectMapper.writeValueAsString(event)));

        return "Liked";
    }
}
```

### Task 2.9: CommentService với AI Moderation
📁 **File:** `content-service/.../comment/service/CommentService.java`
```java
package com.blur.content.comment.service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, 
            String postId, String userId) {
        
        Comment comment = Comment.builder()
            .postId(postId)
            .userId(userId)
            .content(request.getContent())
            .status(CommentStatus.PENDING_MODERATION) // Chờ AI
            .createdAt(Instant.now())
            .build();
        comment = commentRepo.save(comment);

        // Publish for AI moderation
        CommentCreatedEvent event = CommentCreatedEvent.builder()
            .commentId(comment.getId())
            .postId(postId)
            .authorId(userId)
            .content(comment.getContent())
            .build();
        event.initDefaults();
        
        outboxRepo.save(OutboxEvent.create("comment.created", comment.getId(),
            objectMapper.writeValueAsString(event)));

        return CommentResponse.from(comment);
    }
}
```

---

# THÁNG 3: WEBSOCKET MIGRATION

## Tuần 9-10: Remove Socket.IO, Add Spring WebSocket

### Task 3.1: Remove Socket.IO Dependencies
📁 **File:** `chat-service/pom.xml` - XÓA:
```xml
<!-- XÓA dependency này -->
<dependency>
    <groupId>com.corundumstudio.socketio</groupId>
    <artifactId>netty-socketio</artifactId>
</dependency>
```

### Task 3.2: Add Spring WebSocket
📁 **File:** `chat-service/pom.xml` - THÊM:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Task 3.3: WebSocket Config
📁 **File:** `realtime-service/.../config/WebSocketConfig.java`
```java
package com.blur.realtime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Topics cho broadcast
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix cho messages từ client
        config.setApplicationDestinationPrefixes("/app");
        // Prefix cho user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:3000", "http://localhost:5173")
            .withSockJS();
    }
}
```

### Task 3.4: WebSocket Auth Interceptor
📁 **File:** `realtime-service/.../config/WebSocketAuthInterceptor.java`
```java
package com.blur.realtime.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String userId = jwtProvider.getUserIdFromToken(token);
                accessor.setUser(new StompPrincipal(userId));
            }
        }
        return message;
    }
}
```

### Task 3.5: ChatWebSocketController
📁 **File:** `realtime-service/.../websocket/ChatWebSocketController.java`
```java
package com.blur.realtime.websocket;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final ChatMessageService chatService;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/chat.send/{conversationId}")
    public void sendMessage(
            @DestinationVariable String conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        ChatMessage message = chatService.createMessage(
            conversationId, principal.getName(), request);
        
        // Broadcast to conversation
        messaging.convertAndSend(
            "/topic/chat/" + conversationId,
            ChatMessageResponse.from(message)
        );
    }

    @MessageMapping("/chat.typing/{conversationId}")
    public void typing(
            @DestinationVariable String conversationId,
            @Payload TypingRequest request,
            Principal principal) {
        
        messaging.convertAndSend(
            "/topic/chat/" + conversationId + "/typing",
            new TypingEvent(principal.getName(), request.isTyping())
        );
    }
}
```

### Task 3.6: Call/WebRTC Controller
📁 **File:** `realtime-service/.../websocket/CallWebSocketController.java`
```java
package com.blur.realtime.websocket;

@Controller
@RequiredArgsConstructor
public class CallWebSocketController {
    private final SimpMessagingTemplate messaging;
    private final CallService callService;

    @MessageMapping("/call.initiate")
    public void initiateCall(@Payload CallRequest request, Principal principal) {
        CallSession call = callService.createCall(principal.getName(), request);
        
        // Send to target user (private queue)
        messaging.convertAndSendToUser(
            request.getTargetUserId(),
            "/queue/call",
            new IncomingCallEvent(call, principal.getName())
        );
    }

    @MessageMapping("/call.answer")
    public void answerCall(@Payload CallAnswerRequest request, Principal principal) {
        messaging.convertAndSendToUser(
            request.getCallerId(),
            "/queue/call",
            new CallAnsweredEvent(request.getCallId(), request.getSdp())
        );
    }

    @MessageMapping("/call.reject")
    public void rejectCall(@Payload CallRejectRequest request, Principal principal) {
        messaging.convertAndSendToUser(
            request.getCallerId(),
            "/queue/call",
            new CallRejectedEvent(request.getCallId())
        );
    }

    @MessageMapping("/webrtc.signal")
    public void handleSignal(@Payload WebRTCSignal signal, Principal principal) {
        messaging.convertAndSendToUser(
            signal.getTargetUserId(),
            "/queue/call",
            signal
        );
    }
}
```

---

## Tuần 11-12: REALTIME SERVICE + Feed Updates

### Task 3.7: Kafka to WebSocket Bridge
📁 **File:** `realtime-service/.../kafka/EventToWebSocketBridge.java`
```java
package com.blur.realtime.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventToWebSocketBridge {
    private final SimpMessagingTemplate messaging;
    private final UserCacheService userCache;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "post.created", groupId = "realtime-service")
    public void handlePostCreated(String message) throws Exception {
        PostCreatedEvent event = objectMapper.readValue(message, PostCreatedEvent.class);
        
        // Get author's followers
        List<String> followers = userCache.getFollowers(event.getAuthorId());
        
        // Push to each follower's feed
        FeedItem feedItem = FeedItem.fromPost(event);
        for (String followerId : followers) {
            messaging.convertAndSendToUser(followerId, "/queue/feed", feedItem);
        }
    }

    @KafkaListener(topics = "story.created", groupId = "realtime-service")
    public void handleStoryCreated(String message) throws Exception {
        StoryCreatedEvent event = objectMapper.readValue(message, StoryCreatedEvent.class);
        List<String> followers = userCache.getFollowers(event.getAuthorId());
        
        for (String followerId : followers) {
            messaging.convertAndSendToUser(followerId, "/queue/stories", event);
        }
    }

    @KafkaListener(topics = {"post.liked", "user.followed", "comment.created"})
    public void handleNotification(String message) throws Exception {
        BaseEvent event = objectMapper.readValue(message, BaseEvent.class);
        Notification notification = createNotification(event);
        
        messaging.convertAndSendToUser(
            notification.getTargetUserId(),
            "/queue/notifications",
            notification
        );
    }
}
```

### Task 3.8: Online Status Service
📁 **File:** `realtime-service/.../service/OnlineStatusService.java`
```java
package com.blur.realtime.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineStatusService {
    private final RedisTemplate<String, String> redis;
    private final SimpMessagingTemplate messaging;
    private static final String ONLINE_USERS_KEY = "online_users";

    public void setOnline(String userId) {
        redis.opsForSet().add(ONLINE_USERS_KEY, userId);
        broadcastOnlineStatus();
    }

    public void setOffline(String userId) {
        redis.opsForSet().remove(ONLINE_USERS_KEY, userId);
        broadcastOnlineStatus();
    }

    public Set<String> getOnlineUsers() {
        return redis.opsForSet().members(ONLINE_USERS_KEY);
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(ONLINE_USERS_KEY, userId));
    }

    private void broadcastOnlineStatus() {
        Set<String> onlineUsers = getOnlineUsers();
        messaging.convertAndSend("/topic/online", onlineUsers);
    }
}
```

### Task 3.9: WebSocket Session Listener
📁 **File:** `realtime-service/.../websocket/WebSocketEventListener.java`
```java
package com.blur.realtime.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final OnlineStatusService onlineService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            onlineService.setOnline(accessor.getUser().getName());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            onlineService.setOffline(accessor.getUser().getName());
        }
    }
}
```

---

## ✅ CHECKLIST THÁNG 2-3

### Tháng 2
- [ ] USER SERVICE với MySQL + Neo4j
- [ ] CONTENT SERVICE với MongoDB
- [ ] Outbox pattern cho tất cả events
- [ ] Delete IdentityService, profile-service, post-service, story-service cũ

### Tháng 3
- [ ] Remove Socket.IO
- [ ] Spring WebSocket + STOMP config
- [ ] Chat messaging via WebSocket
- [ ] Typing indicator
- [ ] Call/WebRTC signaling
- [ ] Kafka → WebSocket bridge
- [ ] Online status via Redis
- [ ] Real-time feed updates

---

*Tiếp tục: MONTH-4-6-ELASTICSEARCH-TESTING-REPORT.md*
