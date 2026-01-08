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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReplyCommentEventHandler implements EventHandler<Event> {

    RedisTemplate<String, String> redisTemplate;
    SimpMessagingTemplate simpMessagingTemplate;
    JavaMailSender emailSender;
    NotificationService notificationService;
    NotificationWebSocketService notificationWebSocketService;
    ObjectMapper objectMapper;
    RedisService redisService;
    ProfileClient profileClient;

    @Override
    public boolean canHandle(String topic) {
        return topic.equals("user-reply-comment-events");
    }

    @Override
    public void handleEvent(String jsonEvent) throws JsonProcessingException {
        Event event = objectMapper.readValue(jsonEvent, Event.class);
        event.setTimestamp(LocalDateTime.now());

        // ❌ Nếu tự reply chính mình → bỏ qua
        if (event.getSenderId().equals(event.getReceiverId())) {
            log.info("Skip reply notification because sender == receiver, userId={}", event.getSenderId());
            return;
        }

        var profile = profileClient.getProfile(event.getSenderId());

        Notification notification = Notification.builder()
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
                .postId(event.getPostId())
                .read(false)
                .type(Type.Reply)
                .timestamp(event.getTimestamp())
                .content(event.getSenderName() + " đã trả lời bình luận của bạn.")
                .build();

        notificationService.save(notification);

        // ⭐ GỬI TỚI receiverUserId
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
            sendReplyCommentNotification(notification);
        }
    }

    private void sendReplyCommentNotification(Notification notification) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(notification.getReceiverEmail());
            helper.setSubject("🔁 New Reply to Your Comment on Blur!");

            String emailContent =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "    <meta charset=\"UTF-8\">" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                            "    <title>Reply to Your Comment</title>" +
                            "</head>" +
                            "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                            "    <div style=\"background-color: #f5f8fa; padding: 20px;\">" +
                            "        <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">" +
                            "            <div style=\"background-color: #1DA1F2; padding: 30px 20px; text-align: center;\">" +
                            "                <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">Someone Replied to Your Comment!</h1>" +
                            "            </div>" +
                            "            <div style=\"padding: 30px; color: #4a4a4a;\">" +
                            "                <p style=\"font-size: 16px; margin-top: 0;\">Hi <span style=\"font-weight: bold;\">" + notification.getReceiverName() + "</span>,</p>" +
                            "                <div style=\"background-color: #f2f9ff; border-left: 4px solid #1DA1F2; padding: 15px; margin: 20px 0; border-radius: 4px;\">" +
                            "                    <p style=\"margin: 0; font-size: 16px;\">" +
                            "                        <span style=\"font-weight: bold; color: #1DA1F2;\">" + notification.getSenderName() + "</span> has replied to your comment!" +
                            "                    </p>" +
                            "                </div>" +
                            "                <p style=\"font-size: 16px;\">See what they said and keep the conversation going!</p>" +
                            "                <p style=\"color: #777777; font-size: 14px; margin-top: 40px;\">Stay connected and continue sharing your thoughts on Blur!</p>" +
                            "            </div>" +
                            "        </div>" +
                            "    </div>" +
                            "</body>" +
                            "</html>";

            helper.setText(emailContent, true);
            emailSender.send(message);
            log.info("Reply comment notification email sent to {}", notification.getReceiverEmail());
        } catch (Exception e) {
            log.error("Failed to send reply notification email to {}: {}", notification.getReceiverEmail(), e.getMessage(), e);
        }
    }
}