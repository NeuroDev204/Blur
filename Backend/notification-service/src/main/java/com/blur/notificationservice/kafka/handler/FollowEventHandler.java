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
public class FollowEventHandler implements EventHandler<Event>{
    RedisTemplate<String,String> redisTemplate;
    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    NotificationWebSocketService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisService  redisService;
    ProfileClient profileClient;
    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-follow-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        log.info("📥 Received follow event: senderId={}, receiverId={}, receiverUserId={}",
                event.getSenderId(), event.getReceiverId(), event.getReceiverUserId());

        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .senderFirstName(event.getSenderFirstName())
                .senderLastName(event.getSenderLastName())
                .senderImageUrl(event.getSenderImageUrl())
                .receiverId(event.getReceiverId())
                .receiverUserId(event.getReceiverUserId())  // ⭐ THÊM field này vào Notification entity
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .read(false)
                .type(Type.Follow)
                .timestamp(event.getTimestamp())
                .content("bắt đầu theo dõi bạn")
                .build();

        notificationService.save(notification);
        log.info("✅ Notification saved: id={}, type={}", notification.getId(), notification.getType());

        // ✅ GỬI TỚI receiverUserId (identity userId) thay vì receiverId (profileId)
        String targetUserId = event.getReceiverUserId(); // ← userId từ identity-service
        boolean isOnline = redisService.isOnline(targetUserId);
        log.info("🔍 Redis online check for userId {}: {}", targetUserId, isOnline);

        if (isOnline) {
            log.info("📤 Sending WebSocket to /user/{}/queue/notifications", targetUserId);

            simpMessagingTemplate.convertAndSendToUser(
                    targetUserId,  // ⭐ Gửi tới userId, không phải profileId
                    "/queue/notifications",
                    notification
            );

            log.info("✅ WebSocket notification sent successfully");
        } else {
            log.info("📧 User offline, sending email to: {}", event.getReceiverEmail());
            sendFollowNotification(notification);
        }
    }
    private void sendFollowNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("📢 Someone new is following you on Blur!");

            // Build a more attractive HTML email with blue color scheme
            String emailContent =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "    <meta charset=\"UTF-8\">" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "    <title>New Follower on Blur</title>" +
                            "</head>" +
                            "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                            "    <div style=\"background-color: #f5f8fa; padding: 20px;\">" +
                            "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">" +
                            "            <!-- Header -->" +
                            "            <div style=\"background-color: #1DA1F2; padding: 30px 20px; text-align: center;\">" +
                            "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">You Have a New Follower!</h1>" +
                            "            </div>" +
                            "            <!-- Content -->" +
                            "            <div style=\"padding: 30px; color: #4a4a4a;\">" +
                            "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">" + notification.getReceiverName() + "</span>,</p>" +
                            "                <div style=\"background-color: #f2f9ff; border-left: 4px solid #1DA1F2; padding: 15px; margin: 20px 0; border-radius: 4px;\">" +
                            "                    <p style=\"margin: 0; font-size: 16px;\">" +
                            "                        <span style=\"font-weight: bold; color: #1DA1F2;\">" + notification.getSenderName() + "</span> has just started following you on Blur!" +
                            "                    </p>" +
                            "                </div>" +
                            "                <p style=\"font-size: 16px;\">This coul qd be the start of a great connection! Check out their profile and consider following them back.</p>" +
                            "                <div style=\"text-align: center; margin: 30px 0;\">" +
                            "                    <a href=\"http://localhost:3000/profile/user/?profileId=" + notification.getSenderId() + "\" style=\"background-color: #1DA1F2; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 50px; font-weight: bold; display: inline-block; font-size: 16px;\">View Profile</a>" +
                            "                </div>" +
                            "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Keep connecting and expanding your network!</p>" +
                            "            </div>" +

                            "    </div>" +
                            "</body>" +
                            "</html>";

            helper.setText(emailContent, true); // HTML enabled

            emailSender.send(message);
            log.info("Follow email notification sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error("Failed to send follow email notification to {}: {}", notification.getReceiverEmail(), e.getMessage(), e);
        }
    }
}
