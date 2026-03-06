package com.blur.communicationservice.notification.kafka.handler;

import java.time.LocalDateTime;

import jakarta.mail.internet.MimeMessage;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.blur.communicationservice.dto.event.Event;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.enums.NotificationType;
import com.blur.communicationservice.notification.service.NotificationService;
import com.blur.communicationservice.repository.httpclient.ProfileClient;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentEventHandler implements EventHandler<Event> {
    RedisTemplate<String, String> redisTemplate;
    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    WebSocketNotificationService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisCacheService redisService;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-comment-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        var profile = profileClient.getProfile(event.getSenderId());
        log.info("profile: {}", profile);

        Notification notification = Notification.builder()
                .postId(event.getPostId()) // 👈 THÊM DÒNG NÀY
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .receiverId(event.getReceiverId())
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .senderImageUrl(profile.getResult().getImageUrl())
                .read(false)
                .type(NotificationType.CommentPost)
                .timestamp(event.getTimestamp())
                // KHÔNG ghép tên ở đây nữa, chỉ content:
                .content("đã bình luận về bài viết của bạn.")
                .build();

        boolean isOnline = redisService.isUserOnline(event.getReceiverId());
        notificationService.save(notification);

        if (isOnline) {
            notificationWebSocketService.sendNotification(notification);
            simpMessagingTemplate.convertAndSend("/topic/notifications", notification);
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

            // Build a more attractive HTML email with blue color scheme
            String emailContent = "<!DOCTYPE html>" + "<html>"
                    + "<head>"
                    + "    <meta charset=\"UTF-8\">"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                    + "    <title>New Comment on Blur</title>"
                    + "</head>"
                    + "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                    + "    <div style=\"background-color: #f5f8fa; padding: 20px;\">"
                    + "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">"
                    + "            <!-- Header -->"
                    + "            <div style=\"background-color: #1DA1F2; padding: 30px 20px; text-align: center;\">"
                    + "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">New Comment on Your Post!</h1>"
                    + "            </div>"
                    + "            <!-- Content -->"
                    + "            <div style=\"padding: 30px; color: #4a4a4a;\">"
                    + "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">"
                    + notification.getReceiverName() + "</span>,</p>"
                    + "                <div style=\"background-color: #f2f9ff; border-left: 4px solid #1DA1F2; padding: 15px; margin: 20px 0; border-radius: 4px;\">"
                    + "                    <p style=\"margin: 0; font-size: 16px;\">"
                    + "                        <span style=\"font-weight: bold; color: #1DA1F2;\">"
                    + notification.getSenderName() + "</span> has just commented on your post!"
                    + "                    </p>"
                    + "                <p style=\"font-size: 16px;\">Join the conversation and respond to keep the discussion going!</p>"
                    + "                <div style=\"text-align: center; margin: 30px 0;\">"
                    + "                </div>"
                    + "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Stay engaged with your community on Blur!</p>"
                    + "            </div>"
                    + "        </div>"
                    + "    </div>"
                    + "</body>"
                    + "</html>";

            helper.setText(emailContent, true); // HTML enabled
            emailSender.send(message);
            log.info("Comment notification email sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send comment notification email to {}: {}",
                    notification.getReceiverEmail(),
                    e.getMessage(),
                    e);
        }
    }
}
