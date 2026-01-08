package com.postservice.service;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.UserProfileResponse;
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.postservice.dto.event.Event;
import com.postservice.dto.request.CreateCommentRequest;
import com.postservice.dto.response.CommentResponse;
import com.postservice.entity.CommentReply;
import com.postservice.mapper.CommentMapper;
import com.postservice.repository.CommentReplyRepository;
import com.postservice.repository.CommentRepository;
import com.postservice.repository.httpclient.IdentityClient;
import com.postservice.repository.httpclient.NotificationClient;
import com.postservice.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Builder
@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentReplyService {
        CommentReplyRepository commentReplyRepository;
        CommentRepository commentRepository;
        CommentMapper commentMapper;
        ProfileClient profileClient;
        IdentityClient identityClient;
        NotificationClient notificationClient;

        @Caching(evict = {
                        @CacheEvict(value = "commentReplies", key = "#commentId"),
                        @CacheEvict(value = "nestedReplies", key = "#parentReplyId", condition = "#parentReplyId != null")
        })
        public CommentResponse createCommentReply(
                        String commentId,
                        String parentReplyId,
                        CreateCommentRequest commentRequest) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String currentUserId = auth.getName();

                // 1. Tìm comment gốc
                var comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new BlurException(ErrorCode.COMMENT_NOT_FOUND));

                // 2. Nếu reply vào 1 reply khác
                CommentReply parentReply = null;
                if (parentReplyId != null) {
                        parentReply = commentReplyRepository.findById(parentReplyId)
                                        .orElseThrow(() -> new BlurException(ErrorCode.COMMENT_NOT_FOUND));
                }

                // 3. Lấy profile người đang reply (sender)
                ApiResponse<UserProfileResponse> senderProfileRes = profileClient.getProfile(currentUserId);
                var senderProfile = senderProfileRes.getResult();

                String senderFirstName = senderProfile.getFirstName() != null ? senderProfile.getFirstName() : "";
                String senderLastName = senderProfile.getLastName() != null ? senderProfile.getLastName() : "";
                String senderFullName = (senderFirstName + " " + senderLastName).trim();
                String senderImageUrl = senderProfile.getImageUrl();

                if (senderFullName.isEmpty()) {
                        var senderIdentity = identityClient.getUser(currentUserId);
                        senderFullName = senderIdentity.getResult().getUsername();
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
                        // Lấy thông tin sender từ Identity

                        var senderIdentity = identityClient.getUser(currentUserId);

                        // Lấy thông tin receiver
                        var receiverIdentity = identityClient.getUser(receiverUserId);
                        var receiverProfileRes = profileClient.getProfile(receiverUserId);
                        var receiverProfile = receiverProfileRes.getResult();

                        String receiverFirstName = receiverProfile.getFirstName() != null
                                        ? receiverProfile.getFirstName()
                                        : "";
                        String receiverLastName = receiverProfile.getLastName() != null ? receiverProfile.getLastName()
                                        : "";
                        String receiverFullName = (receiverFirstName + " " + receiverLastName).trim();

                        if (receiverFullName.isEmpty()) {
                                receiverFullName = receiverIdentity.getResult().getUsername();
                        }

                        // Tạo Event
                        Event event = Event.builder()
                                        .postId(comment.getPostId())
                                        .senderId(senderIdentity.getResult().getId())
                                        .senderName(senderFullName)
                                        .senderFirstName(senderFirstName)
                                        .senderLastName(senderLastName)
                                        .senderImageUrl(senderImageUrl)
                                        .receiverId(receiverIdentity.getResult().getId())
                                        .receiverName(receiverFullName)
                                        .receiverEmail(receiverIdentity.getResult().getEmail())
                                        .timestamp(LocalDateTime.now())
                                        .build();

                        // GỬI NOTIFICATION QUA FEIGN CLIENT
                        notificationClient.sendReplyCommentNotification(event);

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
                                .orElseThrow(() -> new BlurException(ErrorCode.COMMENT_NOT_FOUND));
                if (!comment.getUserId().equals(userId)) {
                        throw new BlurException(ErrorCode.UNAUTHORIZED);
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
                                .orElseThrow(() -> new BlurException(ErrorCode.COMMENT_NOT_FOUND));
                if (!comment.getUserId().equals(userId)) {
                        throw new BlurException(ErrorCode.UNAUTHORIZED);
                }
                commentReplyRepository.deleteById(comment.getId());
                return "Comment deleted";
        }

        @Cacheable(value = "commentReplies", key = "#commentId", unless = "#result == null || #result.isEmpty()")
        public List<CommentResponse> getAllCommentReplyByCommentId(String commentId) {
                var commentResponses = commentReplyRepository.findAllByCommentId(commentId);
                return commentResponses.stream().map(commentMapper::toCommentResponse)
                                .collect(Collectors.toList());
        }

        @Cacheable(value = "commentReplyById", key = "#commentReplyId", unless = "#result == null")
        public CommentResponse getCommentReplyByCommentReplyId(String commentReplyId) {
                var commentReply = commentReplyRepository.findById(commentReplyId)
                                .orElseThrow(() -> new BlurException(ErrorCode.COMMENT_NOT_FOUND));
                return commentMapper.toCommentResponse(commentReply);
        }

        @Cacheable(value = "nestedReplies", key = "#parentReplyId", unless = "#result == null || #result.isEmpty()")
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