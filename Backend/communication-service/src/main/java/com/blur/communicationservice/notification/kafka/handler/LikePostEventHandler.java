package com.blur.communicationservice.notification.kafka.handler;

import java.time.LocalDateTime;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikePostEventHandler implements EventHandler<Event> {

    JavaMailSender emailSender;
    NotificationService notificationService;
    WebSocketNotificationService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisCacheService redisService;
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
        String senderFullName = String.format(
                        "%s %s",
                        profile.getResult().getFirstName(), profile.getResult().getLastName())
                .trim();
        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderName(senderFullName)
                .senderFirstName(profile.getResult().getFirstName())
                .senderLastName(profile.getResult().getLastName())
                .receiverId(event.getReceiverId())
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .read(false)
                .senderImageUrl(profile.getResult().getImageUrl())
                .type(NotificationType.LikePost)
                .timestamp(event.getTimestamp())
                .content(" like your post.")
                .postId(event.getPostId())
                .build();
        boolean isOnline = redisService.isUserOnline(event.getReceiverId());
        log.info("🔎 Receiver {} online? {}", event.getReceiverId(), isOnline);
        notificationService.save(notification);
        if (isOnline) {
            log.info("📡 Sending realtime notification to {}", notification.getReceiverId());
            notificationWebSocketService.sendNotification(notification);
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

            // Build a more attractive HTML email with blue color scheme
            String emailContent = "<!DOCTYPE html>" + "<html>"
                    + "<head>"
                    + "    <meta charset=\"UTF-8\">"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                    + "    <title>New Like on Your Post</title>"
                    + "</head>"
                    + "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                    + "    <div style=\"background-color: #f5f8fa; padding: 20px;\">"
                    + "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">"
                    + "            <!-- Header -->"
                    + "            <div style=\"background-color: #E91E63; padding: 30px 20px; text-align: center;\">"
                    + "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">Your Post Got Some Love!</h1>"
                    + "            </div>"
                    + "            <!-- Content -->"
                    + "            <div style=\"padding: 30px; color: #4a4a4a;\">"
                    + "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">"
                    + notification.getReceiverName() + "</span>,</p>"
                    + "                <div style=\"background-color: #fff5f8; border-left: 4px solid #E91E63; padding: 15px; margin: 20px 0; border-radius: 4px;\">"
                    + "                    <p style=\"margin: 0; font-size: 16px;\">"
                    + "                        <span style=\"font-weight: bold; color: #E91E63;\">"
                    + notification.getSenderName() + "</span> just liked your post on Blur!"
                    + "                    </p>"
                    + "                </div>"
                    + "            </div>"
                    + "        </div>"
                    + "    </div>"
                    + "</body>"
                    + "</html>";

            helper.setText(emailContent, true); // HTML enabled

            emailSender.send(message);
            log.info("Like post email notification sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send like post email notification to {}: {}",
                    notification.getReceiverEmail(),
                    e.getMessage(),
                    e);
        }
    }
}
