package com.contentservice.post.service;

import com.contentservice.outbox.repository.OutboxRepository;
import com.contentservice.outbox.service.OutboxService;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.PostRequest;
import com.contentservice.post.dto.response.PostResponse;
import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.entity.Post;
import com.contentservice.post.entity.PostLike;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.PostMapper;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.story.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {

    PostRepository postRepository;
    PostMapper postMapper;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;
    OutboxService outboxService;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @Transactional
    public PostResponse createPost(PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var profileResult = profileClient.getProfile(userId).getResult();

        Post post = Post.builder()
                .content(postRequest.getContent())
                .mediaUrls(postRequest.getMediaUrls())
                .userId(userId)
                .profileId(profileResult != null ? profileResult.getId() : null)
                .firstName(profileResult != null ? profileResult.getFirstName() : null)
                .lastName(profileResult != null ? profileResult.getLastName() : null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        post = postRepository.save(post);

        postRepository.linkPostToAuthor(userId, post.getId(), post.getProfileId(), post.getCreatedAt());
        Map<String, Object> eventPayload = Map.of(
                "eventType", "POST_CREATED",
                "postId", post.getId(),
                "authorId", post.getUserId(),
                "content", post.getContent(),
                "mediaUrls", post.getMediaUrls() != null ? post.getMediaUrls() : List.of());
        outboxService.saveEvent("Post", post.getId(), "POST_CREATED", "post-events", eventPayload);
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
        return postMapper.toPostResponse(postRepository.save(post));
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
            String userName = post.getFirstName() + " " + post.getLastName();
            String userImageUrl = null;
            String profileId = post.getProfileId();

            try {
                ApiResponse<UserProfileResponse> response = profileClient.getProfile(post.getUserId());
                UserProfileResponse up = response.getResult();
                if (up != null) {
                    userName = up.getFirstName() + " " + up.getLastName();
                    userImageUrl = up.getImageUrl();
                    profileId = up.getId();
                }
            } catch (Exception ignored) {
            }

            return PostResponse.builder()
                    .id(post.getId())
                    .userId(post.getUserId())
                    .profileId(profileId)
                    .userName(userName)
                    .firstName(post.getFirstName())
                    .lastName(post.getLastName())
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
        // Traverses (user_profile)-[:POSTED]->(Post) graph edge
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

        boolean alreadyLiked = postRepository.hasUserLikedPost(userId, postId);
        if (alreadyLiked) {
            // Toggle: remove the (user_profile)-[:LIKED_POST]->(Post) edge
            postRepository.unlikePost(userId, postId);
            return "Post unliked successfully";
        }

        // Graph: (user_profile)-[:LIKED_POST {createdAt}]->(Post)
        postRepository.likePost(userId, postId, Instant.now());

        try {
            var senderProfile = profileClient.getProfile(userId).getResult();
            var receiverProfile = profileClient.getProfile(post.getUserId()).getResult();

            Event event = Event.builder()
                    .postId(postId)
                    .senderId(userId)
                    .senderName(senderProfile != null
                            ? senderProfile.getFirstName() + " " + senderProfile.getLastName()
                            : "Unknown")
                    .receiverId(post.getUserId())
                    .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                    .receiverName(receiverProfile != null ? receiverProfile.getUsername() : "Unknown")
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationEventPublisher.publishLikeEvent(event);
        } catch (Exception ignored) {
        }

        return "Post liked successfully";
    }

    @Transactional
    public String unlikePost(String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        // Remove the (user_profile)-[:LIKED_POST]->(Post) edge; no-op if absent
        postRepository.unlikePost(userId, postId);
        return "Post unliked successfully";
    }

    /**
     * Traverses (user_profile)-[:LIKED_POST]->(Post) edges to list who liked a
     * post.
     */
    public List<PostLike> getPostLikesByPostId(String postId) {
        return postRepository.findLikesByPostId(postId);
    }

    public List<PostResponse> getPostsByUserId(String userId) {
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(postMapper::toPostResponse)
                .collect(Collectors.toList());
    }

    public PostResponse getPostById(String postId) {
        return postMapper.toPostResponse(postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND)));
    }

    private void publishPostCreatedEvent(Post post) {
        try {
            // lay profile info da co tu create post
            var profileResponse = profileClient.getProfile(post.getUserId());
            String authorUsername = "";
            String authorAvatar = "";
            if (profileResponse != null && profileResponse.getResult() != null) {
                var profile = profileResponse.getResult();
                authorAvatar = profile.getImageUrl() != null ? profile.getImageUrl() : "";
                authorUsername = profile.getUsername() != null ? profile.getUsername() : "";
            }
            // lay danh sach follower ids
            List<String> followerIds = List.of();
            try {
                var followerResponse = profileClient.getFollowerIds(post.getUserId());
                if (followerResponse != null && followerResponse.getResult() != null) {
                    followerIds = followerResponse.getResult();
                }
            } catch (Exception e) {
                log.warn("Failed to get follower IDs for user {}", post.getUserId(), e);
            }
            if (followerIds.isEmpty()) {
                return;
            }
            // build event payload
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "POST_CREATED");
            event.put("postId", post.getId());
            event.put("authorId", post.getUserId());
            event.put("content", post.getContent());
            event.put("mediaUrls", post.getMediaUrls() != null ? post.getMediaUrls() : List.of());
            event.put("authorUsername", authorUsername);
            event.put("authorFirstName", post.getFirstName());
            event.put("authorLastName", post.getLastName());
            event.put("authorAvatar", authorAvatar);
            event.put("followerIds", followerIds);

            // publksh len kafka topic
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("post-events", post.getId(), json);
        } catch (Exception e) {
            log.error("Failed to publish POST_CREATED event for post {}", post.getId(), e);
        }
    }
}
