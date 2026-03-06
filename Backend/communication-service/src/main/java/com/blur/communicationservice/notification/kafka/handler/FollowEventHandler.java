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
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.blur.communicationservice.websocket.service.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FollowEventHandler implements EventHandler<Event> {
    JavaMailSender emailSender;
    NotificationService notificationService;
    WebSocketNotificationService webSocketNotificationService;
    WebSocketSessionManager sessionManager;
    ObjectMapper objectMapper;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-follow-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());
        var profile = profileClient.getProfile(event.getSenderId());

        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .receiverId(event.getReceiverId())
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .read(false)
                .type(NotificationType.Follow)
                .timestamp(event.getTimestamp())
                .senderImageUrl(profile.getResult().getImageUrl())
                .content(event.getSenderName() + " followed you on Blur.")
                .build();

        notificationService.save(notification);

        if (sessionManager.isUserOnline(event.getReceiverId())) {
            webSocketNotificationService.sendNotification(notification);
        } else {
            sendFollowEmail(notification);
        }
    }

    private void sendFollowEmail(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("Someone new is following you on Blur!");

            String emailContent = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
                    + "<body style=\"margin:0;padding:0;font-family:'Segoe UI',sans-serif;\">"
                    + "<div style=\"background:#f5f8fa;padding:20px;\">"
                    + "<div style=\"max-width:600px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 4px 8px rgba(0,0,0,0.05);\">"
                    + "<div style=\"background:#1DA1F2;padding:30px 20px;text-align:center;\">"
                    + "<h1 style=\"color:#fff;margin:0;font-size:24px;\">You Have a New Follower!</h1></div>"
                    + "<div style=\"padding:30px;color:#4a4a4a;\">"
                    + "<p style=\"font-size:16px;\">Hi <b>"
                    + notification.getReceiverName() + "</b>,</p>"
                    + "<div style=\"background:#f2f9ff;border-left:4px solid #1DA1F2;padding:15px;margin:20px 0;border-radius:4px;\">"
                    + "<p style=\"margin:0;font-size:16px;\"><b style=\"color:#1DA1F2;\">"
                    + notification.getSenderName() + "</b> has just started following you on Blur!</p>"
                    + "</div></div></div></div></body></html>";

            helper.setText(emailContent, true);
            emailSender.send(message);
        } catch (Exception e) {
        }
    }
}
