package com.blur.communicationservice.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.event.Event;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.service.NotificationService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;
    NotificationService notificationService;
    WebSocketNotificationService webSocketNotificationService;

    @PostMapping("/follow")
    public ApiResponse<?> sendFollowNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-follow-events", message);
        return ApiResponse.builder().build();
    }

    @PutMapping("/like-post")
    public ApiResponse<?> sendLikePostNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-like-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/comment")
    public ApiResponse<?> sendCommentNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-comment-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/reply-comment")
    public ApiResponse<?> sendReplyCommentNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-reply-comment-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/like-story")
    public ApiResponse<?> sendLikeStoryNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-like-story-events", message);
        return ApiResponse.builder().build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<Notification>> getAllNotificationsByUserId(@PathVariable("userId") String userId) {
        return ApiResponse.<List<Notification>>builder()
                .result(notificationService.getForUser(userId))
                .build();
    }

    @PutMapping("/markAsRead/{notificationId}")
    public ApiResponse<String> markAsRead(@PathVariable("notificationId") String notificationId) {
        return ApiResponse.<String>builder()
                .result(notificationService.markAsRead(notificationId))
                .build();
    }

    @PutMapping("/markAllAsRead")
    public ApiResponse<String> markAllAsRead() {
        return ApiResponse.<String>builder()
                .result(notificationService.markAllAsRead())
                .build();
    }

    @PostMapping("/moderation-update")
    public ApiResponse<String> sendModerationUpdate(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String commentId = request.get("commentId");
        String postId = request.get("postId");
        String status = request.get("status");

        var moderationData = Map.of(
                "commentId", commentId,
                "postId", postId,
                "status", status,
                "timestamp", System.currentTimeMillis());

        webSocketNotificationService.sendModerationUpdate(userId, moderationData);
        return ApiResponse.<String>builder().result("Moderation update sent").build();
    }
}
