package com.blur.notificationservice.kafka.handler;

import com.blur.notificationservice.dto.event.Event;
import com.blur.notificationservice.entity.Notification;
import com.blur.notificationservice.kafka.model.Type;
import com.blur.notificationservice.repository.httpclient.ProfileClient;
import com.blur.notificationservice.service.NotificationService;
import com.blur.notificationservice.service.NotificationWebSocketService;
import com.blur.notificationservice.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Slf4j
public class CommentEventHandler implements EventHandler<Event> {
    RedisTemplate<String,String> redisTemplate;
    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    NotificationWebSocketService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisService redisService;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-comment-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        var profile = profileClient.getProfile(event.getSenderUserId());

        log.info("profile: {}", profile);

        Notification notification = Notification.builder()
                .postId(event.getPostId())
                .senderId(event.getSenderId())
                .senderUserId(event.getSenderUserId())  // ⭐ THÊM
                .senderName(event.getSenderName())
                .senderFirstName(profile.getResult().getFirstName())  // ⭐ THÊM
                .senderLastName(profile.getResult().getLastName())    // ⭐ THÊM
                .receiverId(event.getReceiverId())
                .receiverUserId(event.getReceiverUserId())  // ⭐ THÊM
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .senderImageUrl(profile.getResult().getImageUrl())
                .read(false)
                .type(Type.CommentPost)
                .timestamp(event.getTimestamp())
                .content("đã bình luận về bài viết của bạn.")
                .build();

        notificationService.save(notification);

        // ⭐ GỬI TỚI receiverUserId thay vì receiverId
        String targetUserId = event.getReceiverUserId();
        boolean isOnline = redisService.isOnline(targetUserId);
        log.info("🔍 User {} online status: {}", targetUserId, isOnline);

        if (isOnline) {
            log.info("📤 Sending WebSocket to /user/{}/queue/notifications", targetUserId);
            simpMessagingTemplate.convertAndSendToUser(
                    targetUserId,  // ⭐ userId
                    "/queue/notifications",
                    notification
            );
        } else {
            sendNewCommentNotification(notification);
        }
    }

    private void sendNewCommentNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("💬 New Comment on Your Post on Blur!");

            String emailContent =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "    <meta charset=\"UTF-8\">" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "    <title>New Comment on Blur</title>" +
                            "</head>" +
                            "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                            "    <div style=\"background-color: #f5f8fa; padding: 20px;\">" +
                            "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">" +
                            "            <div style=\"background-color: #1DA1F2; padding: 30px 20px; text-align: center;\">" +
                            "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">New Comment on Your Post!</h1>" +
                            "            </div>" +
                            "            <div style=\"padding: 30px; color: #4a4a4a;\">" +
                            "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">" + notification.getReceiverName() + "</span>,</p>" +
                            "                <div style=\"background-color: #f2f9ff; border-left: 4px solid #1DA1F2; padding: 15px; margin: 20px 0; border-radius: 4px;\">" +
                            "                    <p style=\"margin: 0; font-size: 16px;\">" +
                            "                        <span style=\"font-weight: bold; color: #1DA1F2;\">" + notification.getSenderName() + "</span> has just commented on your post!" +
                            "                    </p>" +
                            "                </div>" +
                            "                <p style=\"font-size: 16px;\">Join the conversation and respond to keep the discussion going!</p>" +
                            "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Stay engaged with your community on Blur!</p>" +
                            "            </div>" +
                            "        </div>" +
                            "    </div>" +
                            "</body>" +
                            "</html>";

            helper.setText(emailContent, true);
            emailSender.send(message);
            log.info("Comment notification email sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error("Failed to send comment notification email to {}: {}", notification.getReceiverEmail(), e.getMessage(), e);
        }
    }
}