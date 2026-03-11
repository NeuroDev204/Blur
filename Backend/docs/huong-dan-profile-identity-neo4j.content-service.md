# Code full: content-service

## `Backend/content-service/pom.xml`
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
    <groupId>com</groupId>
    <artifactId>content-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>post-service</name>
    <description>post-service</description>
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
        <java.version>21</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>


        <spring-cloud.version>2024.0.0</spring-cloud.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-neo4j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <!-- Post Service pom.xml -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${mapstruct.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
        </dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
	</dependencies>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
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
                <version>3.4.3</version> <!-- Khá»›p vá»›i Spring Boot version báº¡n dÃ¹ng -->
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
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
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                            <mainClass>com.contentservice.ContentServiceApplication</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## `Backend/content-service/src/main/java/com/contentservice/post/controller/PostController.java`
```java
package com.contentservice.post.controller;

import com.contentservice.post.dto.request.PostRequest;
import com.contentservice.post.dto.response.PostResponse;
import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.entity.PostLike;
import com.contentservice.post.service.FeedService;
import com.contentservice.post.service.PostSaveService;
import com.contentservice.post.service.PostService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {
  PostService postService;
  PostSaveService postSaveService;
  FeedService feedService;

  @PostMapping("/create")
  public ApiResponse<PostResponse> createPost(@RequestBody PostRequest post) {
    return ApiResponse.<PostResponse>builder()
        .result(postService.createPost(post))
        .build();
  }

  @GetMapping("/my-posts")
  public ApiResponse<List<PostResponse>> getMyPosts() {
    return ApiResponse.<List<PostResponse>>builder()
        .result(postService.getMyPosts())
        .build();
  }


  @PutMapping("/{postId}/like")
  public ApiResponse<String> likePost(@PathVariable String postId) {
    return ApiResponse.<String>builder()
        .result(postService.likePost(postId))
        .build();
  }

  @PutMapping("/{postId}/unlike")
  public ApiResponse<String> unlikePost(@PathVariable String postId) {
    return ApiResponse.<String>builder()
        .result(postService.unlikePost(postId))
        .build();
  }

  @PutMapping("/{postId}/update")
  public ApiResponse<PostResponse> updatePost(@PathVariable String postId,
                                              @RequestBody PostRequest post) {
    return ApiResponse.<PostResponse>builder()
        .result(postService.updatePost(postId, post))
        .build();
  }

  @DeleteMapping("/{postId}/delete")
  public ApiResponse<String> deletePost(@PathVariable String postId) {
    return ApiResponse.<String>builder()
        .result(postService.deletePost(postId))
        .build();
  }

  /*
  @GetMapping("/all")
  public ApiResponse<List<PostResponse>> getAllPosts() {
      return ApiResponse.<List<PostResponse>>builder()
              .result(postService.getAllPosts())
              .build();
  }

   */
  @GetMapping("/feed")
  public ApiResponse<Page<PostFeedItem>> getMyFeed(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Page<PostFeedItem> feed = feedService.getMyFeed(page, size);
    return ApiResponse.<Page<PostFeedItem>>builder()
        .result(feed)
        .build();
  }

  @GetMapping("/all")
  public ApiResponse<Map<String, Object>> getAllPosts(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "5") int limit) {

    Page<PostResponse> postPage = postService.getAllPots(page, limit);

    Map<String, Object> result = new HashMap<>();
    result.put("posts", postPage.getContent());
    result.put("currentPage", postPage.getNumber() + 1);
    result.put("totalPages", postPage.getTotalPages());
    result.put("hasNextPage", postPage.hasNext());

    return ApiResponse.<Map<String, Object>>builder()
        .code(1000)
        .message("OK")
        .result(result)
        .build();
  }

  @GetMapping("/{postId}/likes")
  public ApiResponse<List<PostLike>> getPostLikes(@PathVariable String postId) {
    return ApiResponse.<List<PostLike>>builder()
        .result(postService.getPostLikesByPostId(postId))
        .build();
  }

  @GetMapping("/users/posts/{userId}")
  public ApiResponse<List<PostResponse>> getUserPosts(@PathVariable String userId) {
    var result = postService.getPostsByUserId(userId);
    return ApiResponse.<List<PostResponse>>builder()
        .result(result)
        .build();
  }

  @PostMapping("/save/{postId}")
  public ApiResponse<String> savePost(@PathVariable String postId) {
    return ApiResponse.<String>builder()
        .result(postSaveService.savePost(postId))
        .build();
  }

  @PostMapping("/unsave/{postId}")
  public ApiResponse<String> unsavePost(@PathVariable String postId) {
    return ApiResponse.<String>builder()
        .result(postSaveService.unsavePost(postId))
        .build();
  }

  @GetMapping("/all-saved")
  public ApiResponse<List<PostResponse>> getAllSavedPosts() {
    return ApiResponse.<List<PostResponse>>builder()
        .result(postSaveService.getAllSavedPost())
        .build();
  }

  @GetMapping("/{id}")
  public ApiResponse<PostResponse> getPostById(@PathVariable String id) {
    return ApiResponse.<PostResponse>builder()
        .result(postService.getPostById(id))
        .build();
  }
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/dto/response/UserProfileResponse.java`
```java
package com.contentservice.post.dto.response;

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
public class UserProfileResponse {
    String id;
    String userId;
    String username;
    String firstName;
    String lastName;
    String imageUrl;
    String email;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/Comment.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("comment")
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String postId;
    String userId;
    String firstName;
    String lastName;
    String content;
    String originalContent;

    String moderationStatus;
    String jobId;
    Double moderationConfidence;
    String modelVersion;
    LocalDateTime moderatedAt;
    Instant createdAt;
    Instant updatedAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/CommentLike.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("comment_like")
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CommentLike {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String commentId;
    String userId;
    Instant createdAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/CommentReply.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("comment_reply")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReply {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String userId;
    String postId;
    String userName;
    String content;
    String commentId;
    String parentReplyId;
    Instant createdAt;
    Instant updatedAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/Post.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("post")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String userId;
    String profileId;
    String firstName;
    String lastName;
    String content;
    Instant createdAt;
    Instant updatedAt;
    List<String> mediaUrls;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/PostFeedItem.java`
```java
package com.contentservice.post.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Node("post_feed")
public class PostFeedItem {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    String postId;
    String content;
    List<String> imageUrls;
    String videoUrl;

    String authorId;
    String authorUsername;
    String authorFirstName;
    String authorLastName;
    String authorAvatar;

    int likeCount;
    int commentCount;
    int shareCount;

    String targetUserId;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/PostLike.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("post_like")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostLike {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String postId;
    String userId;
    Instant createdAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/entity/PostSave.java`
```java
package com.contentservice.post.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("post_save")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSave {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String userId;
    String postId;
    Instant savedAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/CommentLikeRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CommentLikeRepository extends Neo4jRepository<CommentLike, String> {
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/CommentReplyRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentReply;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CommentReplyRepository extends Neo4jRepository<CommentReply, String> {
    List<CommentReply> findAllByCommentId(String commentId);

    List<CommentReply> findAllByParentReplyId(String parentReplyId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/CommentRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.Comment;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CommentRepository extends Neo4jRepository<Comment, String> {
    List<Comment> findAllByPostId(String postId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/httpclient/ProfileClient.java`
```java
package com.contentservice.post.repository.httpclient;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.story.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "profile-service", url = "${app.service.profile.url}")
public interface ProfileClient {
  @GetMapping("/internal/users/{userId}")
  ApiResponse<UserProfileResponse> getProfile(@PathVariable("userId") String userId);

  @GetMapping("/profile/users/{profileId}")
  ApiResponse<UserProfileResponse> getProfileByProfileId(@PathVariable String profileId);

  @GetMapping("/internal/user/{userId}/follower-ids")
  ApiResponse<List<String>> getFollowerIds(@PathVariable("userId") String userId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/PostFeedRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends Neo4jRepository<PostFeedItem, String> {
    Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(String targetUserId, Pageable pageable);

    void deleteAllByPostId(String postId);

    void deleteAllByAuthorId(String authorId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/PostLikeRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.PostLike;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends Neo4jRepository<PostLike, String> {
    boolean existsByUserIdAndPostId(String userId, String postId);

    PostLike findByUserIdAndPostId(String userId, String postId);

    List<PostLike> findAllByPostId(String postId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/PostRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.Post;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends Neo4jRepository<Post, String> {
    List<Post> findAllByUserIdOrderByCreatedAtDesc(String userId);

}
```

## `Backend/content-service/src/main/java/com/contentservice/post/repository/PostSaveRepository.java`
```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.PostSave;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSaveRepository extends Neo4jRepository<PostSave, String> {
    PostSave findByPostId(String postId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/service/CommentReplyService.java`
```java
package com.contentservice.post.service;

import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.entity.CommentReply;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.CommentMapper;
import com.contentservice.post.repository.CommentReplyRepository;
import com.contentservice.post.repository.CommentRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Builder
@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentReplyService {
    CommentReplyRepository commentReplyRepository;
    CommentRepository commentRepository;
    CommentMapper commentMapper;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;

    @Caching(evict = {
            @CacheEvict(value = "commentReplies", key = "#commentId"),
            @CacheEvict(value = "nestedReplies", key = "#parentReplyId", condition = "#parentReplyId != null")
    })
    public CommentResponse createCommentReply(
            String commentId,
            String parentReplyId,
            CreateCommentRequest commentRequest
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = auth.getName();

        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        CommentReply parentReply = null;
        if (parentReplyId != null) {
            parentReply = commentReplyRepository.findById(parentReplyId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        } else {
        }

        var senderProfileRes = profileClient.getProfile(currentUserId);
        var senderProfile = senderProfileRes.getResult();

        String senderFullName = buildDisplayName(senderProfile);
        String senderImageUrl = senderProfile != null ? senderProfile.getImageUrl() : null;

        CommentReply commentReply = CommentReply.builder()
                .userId(currentUserId)
                .userName(senderFullName)
                .content(commentRequest.getContent())
                .commentId(comment.getId())
                .parentReplyId(parentReplyId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        commentReply = commentReplyRepository.save(commentReply);

        String receiverUserId;
        if (parentReply != null) {
            receiverUserId = parentReply.getUserId();
        } else {
            receiverUserId = comment.getUserId();
        }

        if (receiverUserId.equals(currentUserId)) {
            return commentMapper.toCommentResponse(commentReply);
        }

        try {
            var receiverProfileRes = profileClient.getProfile(receiverUserId);
            var receiverProfile = receiverProfileRes.getResult();

            Event event = Event.builder()
                    .postId(comment.getPostId())
                    .senderId(currentUserId)
                    .senderName(senderFullName)
                    .senderFirstName(senderProfile != null ? senderProfile.getFirstName() : null)
                    .senderLastName(senderProfile != null ? senderProfile.getLastName() : null)
                    .senderImageUrl(senderImageUrl)
                    .receiverId(receiverUserId)
                    .receiverName(buildDisplayName(receiverProfile))
                    .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationEventPublisher.publishReplyCommentEvent(event);
        } catch (Exception e) {
        }

        return commentMapper.toCommentResponse(commentReply);
    }

    @Caching(evict = {
            @CacheEvict(value = "commentReplies", key = "#root.target.getCommentIdByReplyId(#commentReplyId)"),
            @CacheEvict(value = "nestedReplies", key = "#root.target.getParentReplyId(#commentReplyId)"),
            @CacheEvict(value = "commentReplyById", key = "#commentReplyId")
    })
    public CommentResponse updateCommentReply(String commentReplyId, CreateCommentRequest commentReply) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = auth.getName();
        var comment = commentReplyRepository.findById(commentReplyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        comment.setUpdatedAt(Instant.now());
        comment.setContent(commentReply.getContent());
        return commentMapper.toCommentResponse(commentReplyRepository.save(comment));
    }

    @Caching(evict = {
            @CacheEvict(value = "commentReplies", key = "#root.target.getCommentIdByReplyId(#commentId)"),
            @CacheEvict(value = "nestedReplies", key = "#root.target.getParentReplyId(#commentId)"),
            @CacheEvict(value = "commentReplyById", key = "#commentId")
    })
    public String deleteCommentReply(String commentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = auth.getName();
        var comment = commentReplyRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        commentReplyRepository.deleteById(comment.getId());
        return "Comment deleted";
    }

    @Cacheable(
            value = "commentReplies",
            key = "#commentId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<CommentResponse> getAllCommentReplyByCommentId(String commentId) {
        var commentResponses = commentReplyRepository.findAllByCommentId(commentId);
        return commentResponses.stream().map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "commentReplyById",
            key = "#commentReplyId",
            unless = "#result == null"
    )
    public CommentResponse getCommentReplyByCommentReplyId(String commentReplyId) {
        var commentReply = commentReplyRepository.findById(commentReplyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        return commentMapper.toCommentResponse(commentReply);
    }

    @Cacheable(
            value = "nestedReplies",
            key = "#parentReplyId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<CommentResponse> getRepliesByParentReplyId(String parentReplyId) {
        return commentReplyRepository.findAllByParentReplyId(parentReplyId)
                .stream()
                .map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    public String getCommentIdByReplyId(String replyId) {
        return commentReplyRepository.findById(replyId)
                .map(CommentReply::getCommentId)
                .orElse(null);
    }

    public String getParentReplyId(String replyId) {
        return commentReplyRepository.findById(replyId)
                .map(CommentReply::getParentReplyId)
                .orElse(null);
    }

    private String buildDisplayName(UserProfileResponse profile) {
        if (profile == null) {
            return "Unknown";
        }
        String firstName = profile.getFirstName() != null ? profile.getFirstName() : "";
        String lastName = profile.getLastName() != null ? profile.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return profile.getUsername() != null ? profile.getUsername() : "Unknown";
    }
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/service/CommentService.java`
```java
package com.contentservice.post.service;

import com.contentservice.kafka.ModerationProducer;
import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.entity.Comment;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.CommentMapper;
import com.contentservice.post.repository.CommentRepository;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
    CommentRepository commentRepository;
    CommentMapper commentMapper;
    ModerationProducer moderationProducer;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;
    PostRepository postRepository;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profileRes = profileClient.getProfile(userId);
        var profile = profileRes.getResult();

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .content(request.getContent())
                .createdAt(Instant.now())
                .moderationStatus("PENDING_MODERATION")
                .build();
        comment = commentRepository.save(comment);

        moderationProducer.submit(comment.getId(), postId, userId, request.getContent());

        try {
            var receiverProfile = profileClient.getProfile(post.getUserId()).getResult();

            Event event = Event.builder()
                    .postId(postId)
                    .senderId(userId)
                    .senderName(buildDisplayName(profile))
                    .receiverId(post.getUserId())
                    .receiverName(buildDisplayName(receiverProfile))
                    .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationEventPublisher.publishCommentEvent(event);
        } catch (Exception e) {
        }

        return commentMapper.toCommentResponse(comment);
    }

    @Cacheable(value = "comments", key = "#postId", unless = "#result == null || #result.isEmpty()")
    public List<CommentResponse> getAllCommentByPostId(String postId) {
        return commentRepository.findAllByPostId(postId).stream().map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    public CommentResponse getCommentById(String commentId) {
        return commentMapper.toCommentResponse(commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND)));
    }

    @CacheEvict(value = "comments", key = "#root.target.getPostIdByCommentId(#commentId)")
    public CommentResponse updateComment(String commentId, CreateCommentRequest request) {

        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        if (!comment.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(Instant.now());
        commentRepository.save(comment);
        return commentMapper.toCommentResponse(comment);
    }

    @CacheEvict(value = "comments", key = "#root.target.getPostIdByCommentId(#commentId)")
    public String deleteComment(String commentId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        if (!comment.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        commentRepository.deleteById(comment.getId());
        return "Comment deleted";
    }

    public String getPostIdByCommentId(String commentId) {
        return commentRepository.findById(commentId)
                .map(Comment::getPostId)
                .orElse(null);
    }

    private String buildDisplayName(UserProfileResponse profile) {
        if (profile == null) {
            return "Unknown";
        }
        String firstName = profile.getFirstName() != null ? profile.getFirstName() : "";
        String lastName = profile.getLastName() != null ? profile.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return profile.getUsername() != null ? profile.getUsername() : "Unknown";
    }
}
```

## `Backend/content-service/src/main/java/com/contentservice/post/service/PostService.java`
```java
package com.contentservice.post.service;

import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.PostRequest;
import com.contentservice.post.dto.response.PostResponse;
import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.entity.Post;
import com.contentservice.post.entity.PostLike;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.PostMapper;
import com.contentservice.post.repository.PostLikeRepository;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.story.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostService {
  PostRepository postRepository;
  PostMapper postMapper;
  ProfileClient profileClient;
  PostLikeRepository postLikeRepository;
  NotificationEventPublisher notificationEventPublisher;
  ObjectMapper objectMapper;
  KafkaTemplate<String, String> kafkaTemplate;

  @Transactional
  public PostResponse createPost(PostRequest postRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = authentication.getName();
    var profile = profileClient.getProfile(userId);
    Post post = Post.builder()
        .content(postRequest.getContent())
        .mediaUrls(postRequest.getMediaUrls())
        .userId(userId)
        .firstName(profile.getResult().getFirstName())
        .lastName(profile.getResult().getLastName())
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    post = postRepository.save(post);
    publishPostCreatedEvent(post);
    return postMapper.toPostResponse(post);
  }

  @Transactional
  public PostResponse updatePost(String postId, PostRequest postRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    var userId = authentication.getName();
    if (!post.getUserId().equals(userId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    post.setContent(postRequest.getContent());
    post.setMediaUrls(postRequest.getMediaUrls());
    post.setUpdatedAt(Instant.now());
    post = postRepository.save(post);
    return postMapper.toPostResponse(post);
  }

  @Transactional
  public String deletePost(String postId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    var userId = authentication.getName();
    if (!post.getUserId().equals(userId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    postRepository.deleteById(postId);
    return "Post deleted successfully";
  }

  public Page<PostResponse> getAllPots(int page, int limit) {
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
    Page<Post> postPage = postRepository.findAll(pageable);

    List<PostResponse> responses = postPage.getContent().stream().map(post -> {
      String userName = "Unknown";
      String userImageUrl = null;
      String profileId = null;

      try {
        ApiResponse<UserProfileResponse> response = profileClient.getProfile(post.getUserId());
        UserProfileResponse userProfileResponse = response.getResult();

        if (userProfileResponse != null) {
          userName = buildDisplayName(userProfileResponse);
          userImageUrl = userProfileResponse.getImageUrl();
          profileId = userProfileResponse.getId();
        }
      } catch (Exception e) {
      }

      return PostResponse.builder()
          .id(post.getId())
          .userId(post.getUserId())
          .profileId(profileId)
          .userName(userName)
          .lastName(post.getLastName())
          .firstName(post.getFirstName())
          .userImageUrl(userImageUrl)
          .content(post.getContent())
          .mediaUrls(post.getMediaUrls())
          .createdAt(post.getCreatedAt())
          .updatedAt(post.getUpdatedAt())
          .build();
    }).collect(Collectors.toList());
    return new PageImpl<>(responses, pageable, postPage.getTotalElements());
  }

  public List<PostResponse> getMyPosts() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();
    return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        .stream().map(postMapper::toPostResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public String likePost(String postId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = authentication.getName();

    var post = postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

    if (userId.equals(post.getUserId())) {
      throw new AppException(ErrorCode.CANNOT_LIKE_YOUR_POST);
    }

    PostLike existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

    if (existingLike != null) {
      postLikeRepository.delete(existingLike);
      return "Post unliked successfully";
    } else {
      PostLike like = PostLike.builder()
          .userId(userId)
          .postId(postId)
          .createdAt(Instant.now())
          .build();
      postLikeRepository.save(like);

      try {
        var senderProfile = profileClient.getProfile(userId).getResult();
        var receiverProfile = profileClient.getProfile(post.getUserId()).getResult();

        Event event = Event.builder()
            .postId(postId)
            .senderId(userId)
            .senderName(buildDisplayName(senderProfile))
            .receiverId(post.getUserId())
            .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
            .receiverName(buildDisplayName(receiverProfile))
            .timestamp(LocalDateTime.now())
            .build();

        notificationEventPublisher.publishLikeEvent(event);
      } catch (Exception e) {
      }

      return "Post liked successfully";
    }
  }

  @Transactional
  public String unlikePost(String postId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = authentication.getName();

    PostLike postLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

    if (postLike != null) {
      postLikeRepository.delete(postLike);
    } else {
    }

    return "Post unliked successfully";
  }

  public List<PostLike> getPostLikesByPostId(String postId) {
    List<PostLike> likes = postLikeRepository.findAllByPostId(postId);
    return likes;
  }

  public List<PostResponse> getPostsByUserId(String userId) {
    return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(postMapper::toPostResponse)
        .collect(Collectors.toList());
  }

  public PostResponse getPostById(String postId) {
    return postMapper.toPostResponse(postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND)));
  }

  private void publishPostCreatedEvent(Post post) {
    try {
      String authorUsername = "";
      String authorFirstName = post.getFirstName();
      String authorLastName = post.getLastName();
      String authorAvatar = "";
      try {
        var profileResponse = profileClient.getProfile(post.getUserId());
        if (profileResponse != null && profileResponse.getResult() != null) {
          var profile = profileResponse.getResult();
          authorUsername = profile.getUsername();
          authorFirstName = profile.getFirstName();
          authorLastName = profile.getLastName();
          authorAvatar = profile.getImageUrl() == null ? profile.getImageUrl() : "";
        }
      } catch (Exception e) {
        log.warn("Failed to get profile for user {}", post.getUserId(), e);
      }
      List<String> followerIds = getFollowerIds(post.getUserId());

      Map<String, Object> event = new HashMap<>();
      event.put("eventType", "POST_CREATED");
      event.put("postId", post.getId());
      event.put("authorId", post.getUserId());
      event.put("content", post.getContent());
      event.put("mediaUrls", post.getMediaUrls());
      event.put("authorUsername", authorUsername);
      event.put("authorFirstName", authorFirstName);
      event.put("authorLastName", authorLastName);
      event.put("authorAvatar", authorAvatar);
      event.put("followerIds", followerIds);

      String json = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("post-events", post.getId(), json);
    } catch (Exception e) {
      log.error("Failed to publish to POST_CREATED event for post {} ", post.getId(), e);
    }
  }

  private List<String> getFollowerIds(String userId) {
    try {
      var res = profileClient.getFollowerIds(userId);
      if (res != null && res.getResult() != null) {
        return res.getResult();
      }
    } catch (Exception e) {
      log.warn("Failed to get follower Ids for user {}", userId, e);
    }
    return List.of();
  }

  private String buildDisplayName(UserProfileResponse profile) {
    if (profile == null) {
      return "Unknown";
    }
    String firstName = profile.getFirstName() != null ? profile.getFirstName() : "";
    String lastName = profile.getLastName() != null ? profile.getLastName() : "";
    String fullName = (firstName + " " + lastName).trim();
    if (!fullName.isBlank()) {
      return fullName;
    }
    return profile.getUsername() != null ? profile.getUsername() : "Unknown";
  }
}
```

## `Backend/content-service/src/main/java/com/contentservice/story/entity/Story.java`
```java
package com.contentservice.story.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("story")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Story {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String content;
    String authorId;
    String mediaUrl;
    Instant timestamp;
    String firstName;
    String lastName;
    String thumbnailUrl;
    Instant createdAt;
    Instant updatedAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/story/entity/StoryLike.java`
```java
package com.contentservice.story.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("story_like")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryLike {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String storyId;
    String userId;
    Instant createdAt;
    Instant updatedAt;
}
```

## `Backend/content-service/src/main/java/com/contentservice/story/repository/StoryLikeRepository.java`
```java
package com.contentservice.story.repository;

import com.contentservice.story.entity.StoryLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryLikeRepository extends Neo4jRepository<StoryLike, String> {
    StoryLike findByStoryId(String storyId);

    void deleteByStoryIdAndUserId(String storyId, String userId);
}
```

## `Backend/content-service/src/main/java/com/contentservice/story/repository/StoryRepository.java`
```java
package com.contentservice.story.repository;

import com.contentservice.story.entity.Story;
import java.time.Instant;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryRepository extends Neo4jRepository<Story, String> {
    List<Story> findAllByAuthorId(String authorId);

    @Query("MATCH (s:story) WHERE s.createdAt < $timestamp RETURN s")
    List<Story> findAllByCreatedAtBefore(Instant timestamp);
}
```

## `Backend/content-service/src/main/java/com/contentservice/story/service/StoryLikeService.java`
```java
package com.contentservice.story.service;

import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.story.entity.StoryLike;
import com.contentservice.story.repository.StoryLikeRepository;
import com.contentservice.story.repository.StoryRepository;
import com.contentservice.story.repository.httpclient.ProfileClient;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryLikeService {
    StoryLikeRepository storyLikeRepository;
    StoryRepository storyRepository;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String likeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        StoryLike storyLike = StoryLike.builder()
                .storyId(storyId)
                .userId(userId)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
        storyLikeRepository.save(storyLike);
        try {
            var receiverProfile = profileClient.getProfile(story.getAuthorId()).getResult();
            Event event = Event.builder()
                    .senderName(story.getFirstName() + " " + story.getLastName())
                    .senderId(userId)
                    .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                    .receiverId(story.getAuthorId())
                    .receiverName(buildDisplayName(receiverProfile))
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationEventPublisher.publishLikeStoryEvent(event);
        } catch (Exception e) {
        }
        return "Like story successfully";
    }

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String unlikeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        storyLikeRepository.deleteByStoryIdAndUserId(storyId, userId);
        return "Unlike story successfully";
    }

    private String buildDisplayName(UserProfileResponse profile) {
        if (profile == null) {
            return "Unknown";
        }
        String firstName = profile.getFirstName() != null ? profile.getFirstName() : "";
        String lastName = profile.getLastName() != null ? profile.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return profile.getUsername() != null ? profile.getUsername() : "Unknown";
    }
}
```

## `Backend/content-service/src/main/resources/application.yaml`
```yaml
server:
  port: ${SERVER_PORT:8082}
  address: 0.0.0.0
  servlet:
    context-path: /

spring:
  application:
    name: ${SPRING_APPLICATION_NAME:content-service}

  neo4j:
    uri: ${NEO4J_URI:bolt://localhost:7687}
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD:12345678}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 1
      timeout: ${REDIS_TIME_OUT:60000}
      lettuce:
        pool:
          max-active: ${MAX_ACTIVE:8}
          max-idle: ${MAX_IDLE:8}
          min-idle: ${MIN_IDLE:0}

  cache:
    type: redis
    redis:
      time-to-live: ${TIME_TO_LIVE:600000}
      cache-null-values: false
      use-key-prefix: true
      key-prefix: ${REDIS_KEY_PREFIX:content-service:}
      enable-statistics: true

app:
  service:
    profile:
      url: ${PROFILE_SERVICE_URL:http://localhost:8081}

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9093}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: content-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

