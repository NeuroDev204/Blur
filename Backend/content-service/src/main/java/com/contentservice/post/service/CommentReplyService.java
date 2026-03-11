package com.contentservice.post.service;

import com.contentservice.post.dto.event.Event;
import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.entity.CommentReply;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.CommentMapper;
import com.contentservice.post.repository.CommentReplyRepository;
import com.contentservice.post.repository.CommentRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.kafka.NotificationEventPublisher;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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


        // 1. Tìm comment gốc
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));


        // 2. Nếu reply vào 1 reply khác
        CommentReply parentReply = null;
        if (parentReplyId != null) {
            parentReply = commentReplyRepository.findById(parentReplyId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        } else {
        }

        // 3. Lấy profile người đang reply (sender)
        var senderProfileRes = profileClient.getProfile(currentUserId);
        var senderProfile = senderProfileRes.getResult();

        String senderFirstName = senderProfile.getFirstName() != null ? senderProfile.getFirstName() : "";
        String senderLastName = senderProfile.getLastName() != null ? senderProfile.getLastName() : "";
        String senderFullName = (senderFirstName + " " + senderLastName).trim();
        String senderImageUrl = senderProfile.getImageUrl();

        if (senderFullName.isEmpty()) {
            senderFullName = senderProfile.getUsername();
        }

        // 4. Tạo CommentReply
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

        // 5. Xác định người nhận thông báo
        String receiverUserId;
        if (parentReply != null) {
            receiverUserId = parentReply.getUserId();
        } else {
            receiverUserId = comment.getUserId();
        }

        // 6. Kiểm tra xem có phải tự reply không
        if (receiverUserId.equals(currentUserId)) {
            return commentMapper.toCommentResponse(commentReply);
        }


        try {
            var receiverProfileRes = profileClient.getProfile(receiverUserId);
            var receiverProfile = receiverProfileRes.getResult();

            String receiverFirstName = receiverProfile != null && receiverProfile.getFirstName() != null
                    ? receiverProfile.getFirstName()
                    : "";
            String receiverLastName = receiverProfile != null && receiverProfile.getLastName() != null
                    ? receiverProfile.getLastName()
                    : "";
            String receiverFullName = (receiverFirstName + " " + receiverLastName).trim();

            if (receiverFullName.isEmpty()) {
                receiverFullName = receiverProfile != null ? receiverProfile.getUsername() : "Unknown";
            }

            Event event = Event.builder()
                    .postId(comment.getPostId())
                    .senderId(currentUserId)
                    .senderName(senderFullName)
                    .senderFirstName(senderFirstName)
                    .senderLastName(senderLastName)
                    .senderImageUrl(senderImageUrl)
                    .receiverId(receiverUserId)
                    .receiverName(receiverFullName)
                    .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                    .timestamp(LocalDateTime.now())
                    .build();


            notificationEventPublisher.publishReplyCommentEvent(event);


        } catch (Exception e) {
            // Không throw exception để không làm fail toàn bộ reply action
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
}
