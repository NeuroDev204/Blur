# 📅 THÁNG 4-6: ELASTICSEARCH + TESTING + BÁO CÁO

---

# THÁNG 4: ELASTICSEARCH + SEARCH

## Tuần 13-14: Elasticsearch Setup

### Task 4.1: Docker Compose
📁 **Thêm vào:** `Backend/docker-compose.yml`
```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: blur-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

volumes:
  es_data:
```

### Task 4.2: Elasticsearch Dependencies
📁 **File:** `user-service/pom.xml` - THÊM:
```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-elasticsearch</artifactId>
</dependency>
```

### Task 4.3: User Index Document
📁 **File:** `user-service/.../search/document/UserDocument.java`
```java
package com.blur.user.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
@Setting(settingPath = "/elasticsearch/user-settings.json")
public class UserDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    private String firstName;
    
    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    private String lastName;
    
    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    private String displayName;
    
    @Field(type = FieldType.Keyword)
    private String username;
    
    @Field(type = FieldType.Text)
    private String bio;
    
    @Field(type = FieldType.Keyword)
    private String avatarUrl;
    
    @Field(type = FieldType.Integer)
    private int followerCount;
}
```

### Task 4.4: Vietnamese Analyzer Settings
📁 **File:** `user-service/src/main/resources/elasticsearch/user-settings.json`
```json
{
  "analysis": {
    "analyzer": {
      "vietnamese_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": [
          "lowercase",
          "icu_folding",
          "vietnamese_stop"
        ]
      }
    },
    "filter": {
      "vietnamese_stop": {
        "type": "stop",
        "stopwords": ["và", "của", "là", "trong", "với", "có", "được", "cho"]
      }
    }
  }
}
```

### Task 4.5: User Search Repository
📁 **File:** `user-service/.../search/repository/UserSearchRepository.java`
```java
package com.blur.user.search.repository;

import com.blur.user.search.document.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.annotations.Query;
import java.util.List;

public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {
    
    // Fuzzy search by name
    @Query("""
        {
            "bool": {
                "should": [
                    {"match": {"firstName": {"query": "?0", "fuzziness": "AUTO"}}},
                    {"match": {"lastName": {"query": "?0", "fuzziness": "AUTO"}}},
                    {"match": {"displayName": {"query": "?0", "fuzziness": "AUTO"}}},
                    {"match": {"username": {"query": "?0", "fuzziness": "AUTO"}}}
                ]
            }
        }
    """)
    List<UserDocument> searchByName(String query);
    
    // Autocomplete
    List<UserDocument> findByDisplayNameContainingIgnoreCase(String prefix);
}
```

### Task 4.6: User Search Service
📁 **File:** `user-service/.../search/service/UserSearchService.java`
```java
package com.blur.user.search.service;

import com.blur.user.search.document.UserDocument;
import com.blur.user.search.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchService {
    private final UserSearchRepository searchRepo;

    public List<UserDocument> search(String query) {
        return searchRepo.searchByName(query);
    }

    public List<UserDocument> autocomplete(String prefix) {
        return searchRepo.findByDisplayNameContainingIgnoreCase(prefix);
    }

    public void indexUser(UserDocument user) {
        searchRepo.save(user);
    }

    public void deleteUser(String userId) {
        searchRepo.deleteById(userId);
    }
}
```

### Task 4.7: Kafka Consumer để Sync Index
📁 **File:** `user-service/.../search/kafka/UserIndexConsumer.java`
```java
package com.blur.user.search.kafka;

import com.blur.common.event.UserCreatedEvent;
import com.blur.common.event.UserUpdatedEvent;
import com.blur.user.search.document.UserDocument;
import com.blur.user.search.service.UserSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIndexConsumer {
    private final UserSearchService searchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.created", groupId = "user-search-indexer")
    public void handleUserCreated(String message) throws Exception {
        UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
        
        UserDocument doc = UserDocument.builder()
            .id(event.getUserId())
            .firstName(event.getFirstName())
            .lastName(event.getLastName())
            .displayName(event.getDisplayName())
            .username(event.getUsername())
            .build();
        
        searchService.indexUser(doc);
    }

    @KafkaListener(topics = "user.updated", groupId = "user-search-indexer")
    public void handleUserUpdated(String message) throws Exception {
        UserUpdatedEvent event = objectMapper.readValue(message, UserUpdatedEvent.class);
        
        UserDocument doc = UserDocument.builder()
            .id(event.getUserId())
            .firstName(event.getFirstName())
            .lastName(event.getLastName())
            .displayName(event.getDisplayName())
            .bio(event.getBio())
            .avatarUrl(event.getAvatarUrl())
            .build();
        
        searchService.indexUser(doc);
    }
}
```

### Task 4.8: Search Controller
📁 **File:** `user-service/.../search/controller/SearchController.java`
```java
package com.blur.user.search.controller;

import com.blur.common.dto.response.ApiResponse;
import com.blur.user.search.document.UserDocument;
import com.blur.user.search.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final UserSearchService searchService;

    @GetMapping("/users")
    public ApiResponse<List<UserDocument>> searchUsers(@RequestParam String q) {
        return ApiResponse.<List<UserDocument>>builder()
            .result(searchService.search(q))
            .build();
    }

    @GetMapping("/users/autocomplete")
    public ApiResponse<List<UserDocument>> autocomplete(@RequestParam String prefix) {
        return ApiResponse.<List<UserDocument>>builder()
            .result(searchService.autocomplete(prefix))
            .build();
    }
}
```

---

# THÁNG 5: TESTING

## Tuần 17-18: Unit Tests

### Task 5.1: Test Dependencies
📁 **File:** Thêm vào mỗi service pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

### Task 5.2: PostService Unit Test
📁 **File:** `content-service/src/test/java/.../PostServiceTest.java`
```java
package com.blur.content.post.service;

import com.blur.common.outbox.OutboxEvent;
import com.blur.common.outbox.OutboxRepository;
import com.blur.content.post.entity.Post;
import com.blur.content.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock private PostRepository postRepo;
    @Mock private OutboxRepository outboxRepo;
    @Mock private ObjectMapper objectMapper;
    
    @InjectMocks private PostService postService;
    
    @Captor private ArgumentCaptor<OutboxEvent> outboxCaptor;

    @Test
    void createPost_shouldSaveAndPublishEvent() throws Exception {
        // Given
        PostRequest request = new PostRequest("Hello World");
        String userId = "user123";
        
        Post savedPost = Post.builder()
            .id("post123")
            .content("Hello World")
            .userId(userId)
            .build();
        
        when(postRepo.save(any())).thenReturn(savedPost);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        PostResponse result = postService.createPost(request, userId);

        // Then
        assertNotNull(result);
        assertEquals("post123", result.getId());
        
        verify(postRepo).save(any(Post.class));
        verify(outboxRepo).save(outboxCaptor.capture());
        
        OutboxEvent event = outboxCaptor.getValue();
        assertEquals("post.created", event.getTopic());
        assertEquals("post123", event.getAggregateId());
    }
}
```

### Task 5.3: CommentService Test với AI Moderation
📁 **File:** `content-service/src/test/java/.../CommentServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock private CommentRepository commentRepo;
    @Mock private OutboxRepository outboxRepo;
    @Mock private ObjectMapper objectMapper;
    
    @InjectMocks private CommentService commentService;

    @Test
    void createComment_shouldHavePendingStatus() throws Exception {
        // Given
        CreateCommentRequest request = new CreateCommentRequest("Nice post!");
        String postId = "post123";
        String userId = "user456";
        
        when(commentRepo.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId("comment789");
            return c;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        CommentResponse result = commentService.createComment(request, postId, userId);

        // Then
        assertEquals(CommentStatus.PENDING_MODERATION, result.getStatus());
        verify(outboxRepo).save(argThat(e -> e.getTopic().equals("comment.created")));
    }
}
```

---

## Tuần 19-20: Integration Tests

### Task 5.4: Testcontainers Setup
📁 **File:** `content-service/pom.xml` - THÊM:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Task 5.5: Integration Test Base
📁 **File:** `content-service/src/test/java/.../BaseIntegrationTest.java`
```java
package com.blur.content;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### Task 5.6: Post API Integration Test
📁 **File:** `content-service/src/test/java/.../PostControllerIntegrationTest.java`
```java
package com.blur.content.post.controller;

import com.blur.content.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PostControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired private MockMvc mockMvc;

    @Test
    void createPost_shouldReturn200AndPublishEvent() throws Exception {
        mockMvc.perform(post("/create")
                .contentType("application/json")
                .header("Authorization", "Bearer " + getTestToken())
                .content("""
                    {"content": "Integration test post"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").value("Integration test post"));
    }

    @Test
    void getAllPosts_shouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/all")
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.posts").isArray());
    }
}
```

---

# THÁNG 6: BÁO CÁO

## Checklist nội dung báo cáo

### Chương 1: Giới thiệu (10 trang)
- [ ] Đặt vấn đề: Toxic comments trên MXH
- [ ] Mục tiêu đề tài
- [ ] Phạm vi nghiên cứu
- [ ] Phương pháp nghiên cứu

### Chương 2: Cơ sở lý thuyết (20 trang)
- [ ] Microservices Architecture
- [ ] Event-Driven Architecture với Apache Kafka
- [ ] WebSocket và real-time communication
- [ ] PhoBERT và NLP cho tiếng Việt
- [ ] Elasticsearch và full-text search

### Chương 3: Phân tích thiết kế (20 trang)
- [ ] Use Case Diagram
- [ ] Architecture Diagram
- [ ] Sequence Diagrams (AI moderation, Chat, Feed)
- [ ] ERD Diagrams
- [ ] Class Diagrams

### Chương 4: Triển khai (25 trang)
- [ ] Cấu trúc project
- [ ] Kafka event flow
- [ ] AI integration via Kafka
- [ ] WebSocket implementation
- [ ] Search implementation
- [ ] Testing strategy

### Chương 5: Kết quả & Đánh giá (15 trang)
- [ ] Demo screenshots
- [ ] AI accuracy metrics (Precision, Recall, F1)
- [ ] Performance benchmarks
- [ ] So sánh REST vs Event-Driven
- [ ] Test coverage report

### Chương 6: Kết luận (5 trang)
- [ ] Tóm tắt kết quả
- [ ] Hạn chế
- [ ] Hướng phát triển

---

## ✅ CHECKLIST THÁNG 4-6

### Tháng 4
- [ ] Elasticsearch docker setup
- [ ] Vietnamese analyzer config
- [ ] UserDocument index
- [ ] Kafka → ES sync
- [ ] Search API
- [ ] Autocomplete

### Tháng 5
- [ ] Unit test setup (JUnit 5 + Mockito)
- [ ] PostService tests
- [ ] CommentService tests
- [ ] Integration test setup (Testcontainers)
- [ ] API integration tests
- [ ] Coverage > 70%

### Tháng 6
- [ ] Viết Chương 1-2
- [ ] Viết Chương 3-4
- [ ] Viết Chương 5-6
- [ ] Vẽ diagrams
- [ ] Review và sửa
- [ ] Chuẩn bị demo
