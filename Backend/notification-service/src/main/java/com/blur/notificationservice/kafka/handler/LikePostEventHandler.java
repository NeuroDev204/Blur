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
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikePostEventHandler implements EventHandler<Event> {

    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    NotificationWebSocketService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisService redisService;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-like-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        var profile = profileClient.getProfile(event.getSenderId());
        String senderFullName = String.format("%s %s",
                profile.getResult().getFirstName(),
                profile.getResult().getLastName()
        ).trim();

        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderUserId(event.getSenderUserId())  // ⭐ THÊM
                .senderName(senderFullName)
                .senderFirstName(profile.getResult().getFirstName())
                .senderLastName(profile.getResult().getLastName())
                .receiverId(event.getReceiverId())
                .receiverUserId(event.getReceiverUserId())  // ⭐ THÊM
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .read(false)
                .senderImageUrl(profile.getResult().getImageUrl())
                .type(Type.LikePost)
                .timestamp(event.getTimestamp())
                .content(" like your post.")
                .postId(event.getPostId())
                .build();

        notificationService.save(notification);

        // ⭐ GỬI TỚI receiverUserId
        String targetUserId = event.getReceiverUserId();
        boolean isOnline = redisService.isOnline(targetUserId);
        log.info("🔎 Receiver {} online? {}", targetUserId, isOnline);

        if(isOnline) {
            log.info("📡 Sending realtime notification to user {}", targetUserId);
            simpMessagingTemplate.convertAndSendToUser(
                    targetUserId,  // ⭐ userId
                    "/queue/notifications",
                    notification
            );
        } else {
            sendLikePostNotification(notification);
        }
    }

    private void sendLikePostNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("❤️ Someone liked your post on Blur!");

            String emailContent =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "    <meta charset=\"UTF-8\">" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "    <title>New Like on Your Post</title>" +
                            "</head>" +
                            "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                            "    <div style=\"background-color: #f5f8fa; padding: 20px;\">" +
                            "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">" +
                            "            <div style=\"background-color: #E91E63; padding: 30px 20px; text-align: center;\">" +
                            "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">Your Post Got Some Love!</h1>" +
                            "            </div>" +
                            "            <div style=\"padding: 30px; color: #4a4a4a;\">" +
                            "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">" + notification.getReceiverName() + "</span>,</p>" +
                            "                <div style=\"background-color: #fff5f8; border-left: 4px solid #E91E63; padding: 15px; margin: 20px 0; border-radius: 4px;\">" +
                            "                    <p style=\"margin: 0; font-size: 16px;\">" +
                            "                        <span style=\"font-weight: bold; color: #E91E63;\">" + notification.getSenderName() + "</span> just liked your post on Blur!" +
                            "                    </p>" +
                            "                </div>" +
                            "            </div>" +
                            "        </div>" +
                            "    </div>" +
                            "</body>" +
                            "</html>";

            helper.setText(emailContent, true);
            emailSender.send(message);
            log.info("Like post email notification sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error("Failed to send like post email notification to {}: {}", notification.getReceiverEmail(), e.getMessage(), e);
        }
    }
}