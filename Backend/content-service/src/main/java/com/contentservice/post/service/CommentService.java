package com.contentservice.post.service;

import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.outbox.service.OutboxService;
import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.entity.Comment;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.CommentMapper;
import com.contentservice.post.repository.CommentRepository;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
    CommentRepository commentRepository;
    CommentMapper commentMapper;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;
    PostRepository postRepository;
    OutboxService outboxService;

    @CacheEvict(value = "comments", key = "#postId")
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = profileClient.getProfile(userId).getResult();
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        var receiverProfile = profileClient.getProfile(post.getUserId()).getResult();

        Comment comment = Comment.builder()
                .userId(userId)
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .content(request.getContent())
                .createdAt(Instant.now())
                .moderationStatus("PENDING_MODERATION")
                .build();
        comment = commentRepository.save(comment);

        // Graph: (comment)-[:COMMENTS_ON]->(Post)
        commentRepository.linkCommentToPost(comment.getId(), postId);
        // Graph: (user_profile)-[:COMMENTED {createdAt}]->(comment)
        commentRepository.linkCommentToUser(userId, comment.getId(), comment.getCreatedAt());

        // Send to async moderation via Kafka
        // moderationProducer.submit(comment.getId(), postId, userId,
        // request.getContent());
        Map<String, String> moderationPayload = Map.of(
                "commentId", comment.getId(),
                "postId", postId,
                "userId", userId,
                "content", comment.getContent());
        outboxService.saveEvent("Comment", comment.getId(), "COMMENT_CREATED", "comment-moderation-request",
                moderationPayload);
        String receiverName = receiverProfile != null
                ? receiverProfile.getFirstName() + " " + receiverProfile.getLastName()
                : "Unknown";
        String receiverEmail = receiverProfile != null ? receiverProfile.getEmail() : null;

        Event event = Event.builder()
                .postId(postId)
                .senderId(userId)
                .senderName(profile.getFirstName() + " " + profile.getLastName())
                .receiverId(post.getUserId())
                .receiverName(receiverName)
                .receiverEmail(receiverEmail)
                .timestamp(LocalDateTime.now())
                .build();
        notificationEventPublisher.publishCommentEvent(event);

        return commentMapper.toCommentResponse(comment);
    }

    @Cacheable(value = "comments", key = "#postId", sync = true)
    public List<CommentResponse> getAllCommentByPostId(String postId) {
        // Traverses (comment)-[:COMMENTS_ON]->(Post) graph edge
        return commentRepository.findAllByPostId(postId).stream()
                .map(commentMapper::toCommentResponse)
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
        if (!comment.getUserId().equals(authentication.getName())) {
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
        if (!comment.getUserId().equals(authentication.getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        commentRepository.deleteById(comment.getId());
        return "Comment deleted";
    }

    /**
     * Traverses (comment)-[:COMMENTS_ON]->(Post) to find the parent post ID.
     * Used as a cache-key helper — no stored postId property needed.
     */
    public String getPostIdByCommentId(String commentId) {
        return commentRepository.findPostIdByCommentId(commentId);
    }
}
