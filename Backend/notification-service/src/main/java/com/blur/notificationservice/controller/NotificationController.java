package com.blur.notificationservice.controller;

import com.blur.notificationservice.dto.event.Event;
import com.blur.notificationservice.dto.response.ApiResponse;
import com.blur.notificationservice.entity.Notification;
import com.blur.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper; // ĐỔI SANG JACKSON
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper; // ĐỔI THÀNH OBJECTMAPPER
    NotificationService notificationService;

    @PostMapping("/follow")
    public ApiResponse<?> sendFollowNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event); // DÙNG OBJECTMAPPER
        kafkaTemplate.send("user-follow-events", message);
        return ApiResponse.builder().build();
    }

    @PutMapping("/like-post")
    public ApiResponse<?> sendLikePostNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event); // DÙNG OBJECTMAPPER
        kafkaTemplate.send("user-like-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/comment")
    public ApiResponse<?> sendCommentNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event); // DÙNG OBJECTMAPPER
        kafkaTemplate.send("user-comment-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/reply-comment")
    public ApiResponse<?> sendReplyCommentNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event); // DÙNG OBJECTMAPPER
        kafkaTemplate.send("user-reply-comment-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/like-story")
    public ApiResponse<?> sendLikeStoryNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event); // DÙNG OBJECTMAPPER
        kafkaTemplate.send("user-like-story-events", message);
        return ApiResponse.builder().build();
    }

    @PostMapping("/react-story")
    public ApiResponse<?> sendReactStoryNotification(@RequestBody Event event) throws Exception {
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-react-story-events", message);
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
}