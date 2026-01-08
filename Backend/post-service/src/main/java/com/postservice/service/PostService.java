package com.postservice.service;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.UserProfileResponse;
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.postservice.dto.event.Event;
import com.postservice.dto.request.PostRequest;
import com.postservice.dto.response.PostResponse;
import com.postservice.entity.Post;
import com.postservice.entity.PostLike;
import com.postservice.mapper.PostMapper;
import com.postservice.repository.PostLikeRepository;
import com.postservice.repository.PostRepository;
import com.postservice.repository.httpclient.IdentityClient;
import com.postservice.repository.httpclient.NotificationClient;
import com.postservice.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    PostRepository postRepository;
    PostMapper postMapper;
    ProfileClient profileClient;
    PostLikeRepository postLikeRepository;
    NotificationClient notificationClient;
    IdentityClient identityClient;

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
        return postMapper.toPostResponse(post);
    }

    @Transactional
    public PostResponse updatePost(String postId, PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND));
        var userId = authentication.getName();
        if (!post.getUserId().equals(userId)) {
            throw new BlurException(ErrorCode.UNAUTHORIZED);
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
                .orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND));
        var userId = authentication.getName();
        if (!post.getUserId().equals(userId)) {
            throw new BlurException(ErrorCode.UNAUTHORIZED);
        }
        postRepository.deleteById(postId);
        return "Post deleted successfully";
    }

    public Page<PostResponse> getAllPots(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostResponse> responses = postPage.getContent().stream().map(post -> {
            String userName = "Unknown";
            String userImageUrl = null;
            String profileId = null;

            try {
                ApiResponse<UserProfileResponse> response = profileClient.getProfile(post.getUserId());
                UserProfileResponse userProfileResponse = response.getResult();

                if (userProfileResponse != null) {
                    userName = userProfileResponse.getFirstName() + " " + userProfileResponse.getLastName();
                    userImageUrl = userProfileResponse.getImageUrl();
                    profileId = userProfileResponse.getId();
                }
            } catch (Exception e) {
                log.error("Không lấy được profile cho userId: {}", post.getUserId());
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
        UserProfileResponse userProfile = null;
        try {
            userProfile = profileClient.getProfile(userId).getResult();
        } catch (Exception e) {
            log.error("Error getting profile", e);
        }
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(postMapper::toPostResponse)
                .collect(Collectors.toList());
    }

    // ==================== TOGGLE LIKE/UNLIKE - HOÀN CHỈNH ====================
    @Transactional
    public String likePost(String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND));

        // Không cho tự like bài viết của mình
        if (userId.equals(post.getUserId())) {
            throw new BlurException(ErrorCode.CANNOT_LIKE_YOUR_POST);
        }

        PostLike existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

        if (existingLike != null) {

            postLikeRepository.delete(existingLike);
            log.info("👎 Post unliked - userId: {}, postId: {}", userId, postId);
            return "Post unliked successfully";
        } else {

            PostLike like = PostLike.builder()
                    .userId(userId)
                    .postId(postId)
                    .createdAt(Instant.now())
                    .build();
            postLikeRepository.save(like);

            log.info("👍 Post liked - userId: {}, postId: {}", userId, postId);

            // ✅ GỬI THÔNG BÁO ĐẾN CHỦ BÀI VIẾT (CHỈ KHI LIKE)
            try {
                var sender = identityClient.getUser(userId);
                var receiver = identityClient.getUser(post.getUserId());

                Event event = Event.builder()
                        .postId(postId)
                        .senderId(sender.getResult().getId())
                        .senderName(sender.getResult().getUsername())
                        .receiverId(receiver.getResult().getId())
                        .receiverEmail(receiver.getResult().getEmail())
                        .receiverName(receiver.getResult().getUsername())
                        .timestamp(LocalDateTime.now())
                        .build();

                notificationClient.sendLikePostNotification(event);
                log.info("📧 Notification sent to userId: {}", post.getUserId());
            } catch (Exception e) {
                log.error("❌ Error sending notification", e);
                // Không throw exception, vẫn trả về like thành công
            }

            return "Post liked successfully";
        }
    }

    // ==================== UNLIKE POST (GIỮ LẠI ĐỂ TƯƠNG THÍCH) ====================
    @Transactional
    public String unlikePost(String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();

        log.info("🔍 Unlike Post - userId: {}, postId: {}", userId, postId);

        PostLike postLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

        if (postLike != null) {
            postLikeRepository.delete(postLike);
            log.info("✅ Post unliked - userId: {}, postId: {}", userId, postId);
        } else {
            log.warn("⚠️ PostLike not found - userId: {}, postId: {}", userId, postId);
        }

        return "Post unliked successfully";
    }

    public List<PostLike> getPostLikesByPostId(String postId) {
        List<PostLike> likes = postLikeRepository.findAllByPostId(postId);
        log.info("📊 Get likes for postId: {} - count: {}", postId, likes.size());
        return likes;
    }

    public List<PostResponse> getPostsByUserId(String userId) {
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());
    }
    public PostResponse getPostById(String postId) {
        return postMapper.toPostResponse(postRepository.findById(postId).orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND)));
    }
}