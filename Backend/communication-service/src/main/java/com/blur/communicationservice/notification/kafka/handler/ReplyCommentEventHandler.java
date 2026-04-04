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
import com.blur.communicationservice.repository.httpclient.ProfileClient;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplyCommentEventHandler implements EventHandler<Event> {

    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    WebSocketNotificationService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisCacheService redisService;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-reply-comment-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        // ❌ Nếu tự reply chính mình → bỏ qua, không tạo noti
        if (event.getSenderId().equals(event.getReceiverId())) {
            return;
        }

        var profile = profileClient.getProfile(event.getSenderId());

        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .receiverId(event.getReceiverId())
                .receiverName(event.getReceiverName())
                .receiverEmail(event.getReceiverEmail())
                .senderImageUrl(profile.getResult().getImageUrl())
                .postId(event.getPostId()) // 🔥 Quan trọng: gắn postId vào noti
                .read(false)
                .type(NotificationType.Reply)
                .timestamp(event.getTimestamp())
                .content(event.getSenderName() + " đã trả lời bình luận của bạn.") // text gọn gàng
                .build();

        boolean isOnline = redisService.isUserOnline(event.getReceiverId());
        notificationService.save(notification);

        if (isOnline) {
            // Gửi qua WebSocket
            notificationWebSocketService.sendNotification(notification);
            simpMessagingTemplate.convertAndSend("/topic/notifications", notification);
        } else {
            // Gửi email
            sendReplyCommentNotification(notification);
        }
    }

    private void sendReplyCommentNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("🔁 New Reply to Your Comment on Blur!");

            String emailContent = "<!DOCTYPE html>" + "<html>"
                    + "<head>"
                    + "    <meta charset=\"UTF-8\">"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                    + "    <title>Reply to Your Comment</title>"
                    + "</head>"
                    + "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                    + "    <div style=\"background-color: #f5f8fa; padding: 20px;\">"
                    + "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">"
                    + "            <div style=\"background-color: #1DA1F2; padding: 30px 20px; text-align: center;\">"
                    + "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">Someone Replied to Your Comment!</h1>"
                    + "            </div>"
                    + "            <div style=\"padding: 30px; color: #4a4a4a;\">"
                    + "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">"
                    + notification.getReceiverName() + "</span>,</p>"
                    + "                <div style=\"background-color: #f2f9ff; border-left: 4px solid #1DA1F2; padding: 15px; margin: 20px 0; border-radius: 4px;\">"
                    + "                    <p style=\"margin: 0; font-size: 16px;\">"
                    + "                        <span style=\"font-weight: bold; color: #1DA1F2;\">"
                    + notification.getSenderName() + "</span> has replied to your comment!" + "                    </p>"
                    + "                </div>"
                    + "                <p style=\"font-size: 16px;\">See what they said and keep the conversation going!</p>"
                    + "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Stay connected and continue sharing your thoughts on Blur!</p>"
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
