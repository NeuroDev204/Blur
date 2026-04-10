package com.blur.communicationservice.notification.kafka.handler;

import java.time.LocalDateTime;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.blur.communicationservice.dto.event.Event;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.enums.NotificationType;
import com.blur.communicationservice.notification.service.NotificationService;
import com.blur.communicationservice.resilience.ResilientUserServiceClient;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikeStoryEvenHandler implements EventHandler<Event> {
    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    WebSocketNotificationService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisCacheService redisService;
    ResilientUserServiceClient userServiceClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-like-story-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());
        var profile = userServiceClient.getProfile(event.getSenderId());
        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .receiverId(event.getReceiverId())
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .senderImageUrl(profile.getResult().getImageUrl())
                .read(false)
                .type(NotificationType.LikeStory)
                .timestamp(event.getTimestamp())
                .content(event.getSenderName() + " like your story on Blur.")
                .build();
        boolean isOnline = redisService.isUserOnline(event.getReceiverId());
        notificationService.save(notification);
        if (isOnline) {
            notificationWebSocketService.sendNotification(notification);
            simpMessagingTemplate.convertAndSend("/topic/notifications", notification);
        } else {
            sendStoryLikeNotification(notification);
        }
    }

    private void sendStoryLikeNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("❤️ Someone Liked Your Story on Blur!");

            // Build HTML content for story like notification
            String emailContent = "<!DOCTYPE html>" + "<html>"
                    + "<head>"
                    + "    <meta charset=\"UTF-8\">"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                    + "    <title>Your Story Got a Like</title>"
                    + "</head>"
                    + "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                    + "    <div style=\"background-color: #f5f8fa; padding: 20px;\">"
                    + "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">"
                    + "            <!-- Header -->"
                    + "            <div style=\"background: linear-gradient(135deg, #ff6b6b, #ff8e8e); padding: 30px 20px; text-align: center;\">"
                    + "                <div style=\"font-size: 48px; margin-bottom: 10px;\">❤️</div>"
                    + "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">Your Story Got Some Love!</h1>"
                    + "            </div>"
                    + "            <!-- Content -->"
                    + "            <div style=\"padding: 30px; color: #4a4a4a;\">"
                    + "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">"
                    + notification.getReceiverName() + "</span>,</p>"
                    + "                <div style=\"background-color: #fff5f5; border-left: 4px solid #ff6b6b; padding: 15px; margin: 20px 0; border-radius: 4px;\">"
                    + "                    <p style=\"margin: 0; font-size: 16px;\">"
                    + "                        <span style=\"font-weight: bold; color: #ff6b6b;\">"
                    + notification.getSenderName() + "</span> liked your story!" + "                    </p>"
                    + "                </div>"
                    + "                <p style=\"font-size: 16px;\">Your content is making an impact! Keep sharing your amazing stories with the Blur community.</p>"
                    + "                <div style=\"text-align: center; margin: 30px 0;\">"
                    + "                    <div style=\"display: inline-block; background-color: #ff6b6b; color: white; padding: 12px 24px; border-radius: 25px; text-decoration: none; font-weight: bold; font-size: 14px;\">"
                    + "                        📖 View Your Story"
                    + "                    </div>"
                    + "                </div>"
                    + "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Thank you for being part of the Blur community and sharing your stories with us!</p>"
                    + "            </div>"
                    + "            <!-- Footer -->"
                    + "            <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;\">"
                    + "                <p style=\"margin: 0; color: #6c757d; font-size: 12px;\">This notification was sent from Blur. Keep creating amazing content!</p>"
                    + "            </div>"
                    + "        </div>"
                    + "    </div>"
                    + "</body>"
                    + "</html>";
            helper.setText(emailContent, true);

            emailSender.send(message);

        } catch (Exception e) {

        }
    }
}
