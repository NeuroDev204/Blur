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

        // Validate that the parent comment exists
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        // Validate parent reply if provided
        CommentReply parentReply = null;
        if (parentReplyId != null) {
            parentReply = commentReplyRepository.findById(parentReplyId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        }

        var senderProfile = profileClient.getProfile(currentUserId).getResult();
        String senderFirstName = senderProfile.getFirstName() != null ? senderProfile.getFirstName() : "";
        String senderLastName = senderProfile.getLastName() != null ? senderProfile.getLastName() : "";
        String senderFullName = (senderFirstName + " " + senderLastName).trim();
        String senderImageUrl = senderProfile.getImageUrl();
        if (senderFullName.isEmpty()) senderFullName = senderProfile.getUsername();

        CommentReply commentReply = CommentReply.builder()
                .userId(currentUserId)
                .userName(senderFullName)
                .content(commentRequest.getContent())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        commentReply = commentReplyRepository.save(commentReply);

        // Graph: (comment_reply)-[:REPLIES_TO]->(comment)
        commentReplyRepository.linkReplyToComment(commentReply.getId(), commentId);
        // Graph: (user_profile)-[:REPLIED {createdAt}]->(comment_reply)
        commentReplyRepository.linkReplyToUser(currentUserId, commentReply.getId(), commentReply.getCreatedAt());

        if (parentReplyId != null) {
            // Graph: (comment_reply)-[:NESTED_REPLY_OF]->(parent comment_reply)
            commentReplyRepository.linkReplyToParentReply(commentReply.getId(), parentReplyId);
        }

        // Determine notification receiver
        String receiverUserId = (parentReply != null) ? parentReply.getUserId() : comment.getUserId();
        if (receiverUserId.equals(currentUserId)) {
            return commentMapper.toCommentResponse(commentReply);
        }

        try {
            var receiverProfile = profileClient.getProfile(receiverUserId).getResult();
            String receiverFirstName = receiverProfile != null && receiverProfile.getFirstName() != null
                    ? receiverProfile.getFirstName() : "";
            String receiverLastName = receiverProfile != null && receiverProfile.getLastName() != null
                    ? receiverProfile.getLastName() : "";
            String receiverFullName = (receiverFirstName + " " + receiverLastName).trim();
            if (receiverFullName.isEmpty())
                receiverFullName = receiverProfile != null ? receiverProfile.getUsername() : "Unknown";

            // Traverse graph to get the post id for the notification event
            String postId = commentRepository.findPostIdByCommentId(commentId);

            Event event = Event.builder()
                    .postId(postId)
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
        } catch (Exception ignored) {
        }

        return commentMapper.toCommentResponse(commentReply);
    }

    @Caching(evict = {
            @CacheEvict(value = "commentReplies", key = "#root.target.getCommentIdByReplyId(#commentReplyId)"),
            @CacheEvict(value = "nestedReplies", key = "#root.target.getParentReplyId(#commentReplyId)"),
            @CacheEvict(value = "commentReplyById", key = "#commentReplyId")
    })
    public CommentResponse updateCommentReply(String commentReplyId, CreateCommentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var reply = commentReplyRepository.findById(commentReplyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!reply.getUserId().equals(auth.getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        reply.setUpdatedAt(Instant.now());
        reply.setContent(request.getContent());
        return commentMapper.toCommentResponse(commentReplyRepository.save(reply));
    }

    @Caching(evict = {
            @CacheEvict(value = "commentReplies", key = "#root.target.getCommentIdByReplyId(#commentReplyId)"),
            @CacheEvict(value = "nestedReplies", key = "#root.target.getParentReplyId(#commentReplyId)"),
            @CacheEvict(value = "commentReplyById", key = "#commentReplyId")
    })
    public String deleteCommentReply(String commentReplyId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var reply = commentReplyRepository.findById(commentReplyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!reply.getUserId().equals(auth.getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        commentReplyRepository.deleteById(reply.getId());
        return "Comment deleted";
    }

    @Cacheable(value = "commentReplies", key = "#commentId", unless = "#result == null || #result.isEmpty()")
    public List<CommentResponse> getAllCommentReplyByCommentId(String commentId) {
        // Traverses (comment_reply)-[:REPLIES_TO]->(comment) graph edge
        return commentReplyRepository.findAllByCommentId(commentId)
                .stream().map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "commentReplyById", key = "#commentReplyId", unless = "#result == null")
    public CommentResponse getCommentReplyByCommentReplyId(String commentReplyId) {
        return commentMapper.toCommentResponse(commentReplyRepository.findById(commentReplyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND)));
    }

    @Cacheable(value = "nestedReplies", key = "#parentReplyId", unless = "#result == null || #result.isEmpty()")
    public List<CommentResponse> getRepliesByParentReplyId(String parentReplyId) {
        // Traverses (comment_reply)-[:NESTED_REPLY_OF]->(parent) graph edge
        return commentReplyRepository.findAllByParentReplyId(parentReplyId)
                .stream().map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    /** Traverses (comment_reply)-[:REPLIES_TO]->(comment) — cache-key helper. */
    public String getCommentIdByReplyId(String replyId) {
        return commentReplyRepository.findCommentIdByReplyId(replyId);
    }

    /** Traverses (comment_reply)-[:NESTED_REPLY_OF]->(parent) — cache-key helper. */
    public String getParentReplyId(String replyId) {
        return commentReplyRepository.findParentReplyIdByReplyId(replyId);
    }
}
