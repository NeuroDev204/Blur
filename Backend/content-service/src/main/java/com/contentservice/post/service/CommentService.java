package com.contentservice.post.service;

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
import com.contentservice.kafka.ModerationProducer;
import com.contentservice.kafka.NotificationEventPublisher;
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
import java.util.stream.Collectors;

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
        // Lấy user hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        // Lấy profile của người comment (dùng cho comment + senderName)
        var profileRes = profileClient.getProfile(userId);
        var profile = profileRes.getResult();
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        // // Tạo comment
        // Comment comment = Comment.builder()
        // .content(request.getContent())
        // .userId(userId)
        // .firstName(profile.getFirstName())
        // .lastName(profile.getLastName())
        // .postId(postId)
        // .moderationStatus(ModerationStatus.PENDING_MODERATION)
        // .createdAt(Instant.now())
        // .updatedAt(Instant.now())
        // .build();

        // comment = commentRepository.save(comment);

        // if (post.getUserId().equals(userId)) {
        // return commentMapper.toCommentResponse(comment);
        // }

        // // Lấy info chủ bài viết (receiver) từ Identity
        var receiverProfile = profileClient.getProfile(post.getUserId()).getResult();

        // // Build Event giống kiểu like
        // Event event = Event.builder()
        // .postId(post.getId())
        // .senderId(userId)
        // .senderName(profile.getFirstName() + " " + profile.getLastName())
        // .receiverId(receiver.getId())
        // .receiverName(receiverProfile.getResult().getFirstName() + " " +
        // receiverProfile.getResult().getLastName())
        // .receiverEmail(receiver.getEmail())
        // .timestamp(LocalDateTime.now())
        // .build();

        // notificationEventPublisher.publishCommentEvent(event);

        // return commentMapper.toCommentResponse(comment);
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
        // gui di moderation async qua kafka
        moderationProducer.submit(comment.getId(), postId, userId, request.getContent());
        // Build Event giống kiểu like
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
}
