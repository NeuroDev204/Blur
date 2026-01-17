package com.blur.notificationservice.kafka.consumer;

import com.blur.common.event.CommentCreatedEvent;
import com.blur.notificationservice.entity.Notification;
import com.blur.notificationservice.kafka.model.Type;
import com.blur.notificationservice.service.IdempotencyService;
import com.blur.notificationservice.service.NotificationService;
import com.blur.notificationservice.service.NotificationWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationEventConsumer {
  NotificationService notificationService;
  IdempotencyService idempotencyService;
  ObjectMapper objectMapper;
  NotificationWebSocketService notificationWebSocketService;

  @KafkaListener(topics = "comment.created", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
  public void handleCommentCreated(String message, Acknowledgment ack) {
    try {
      log.info("📩 Received comment.created event: {}", message);

      CommentCreatedEvent event = objectMapper.readValue(message, CommentCreatedEvent.class);

      // tranh xu ly trong lap khi kafka retry
      if (idempotencyService.isProcessed(event.getEventId())) {
        log.info("⏭️ Event {} already processed, skipping", event.getEventId());
        ack.acknowledge(); // commit offset, khong reprocess
        return;
      }

      // tao notification - su dung thong tin tu event (da duoc post-service gui san)
      Notification notification = Notification.builder()
          .postId(event.getPostId())
          .senderId(event.getAuthorId())
          .senderName(event.getAuthorName())
          .senderFirstName(event.getAuthorFirstName())
          .senderLastName(event.getAuthorLastName())
          .senderImageUrl(event.getAuthorImageUrl())
          .receiverId(event.getPostOwnerId())
          .receiverEmail(event.getPostOwnerEmail())
          .receiverName(event.getPostOwnerName())
          .content(event.getAuthorName() + " đã bình luận bài viết của bạn!")
          .type(Type.CommentPost)
          .read(false)
          .timestamp(LocalDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault()))
          .build();

      // luu vao db
      notificationService.save(notification);
      log.info("💾 Notification saved for user {}", event.getPostOwnerId());

      // push realtime qua websocket
      notificationWebSocketService.pushToUser(event.getPostOwnerId(), notification);
      log.info("📤 WebSocket notification pushed to user {}", event.getPostOwnerId());

      // danh dau da xu ly
      idempotencyService.markProcessed(event.getEventId());
      // commit offset chi sau khi xy ly thanh cong
      ack.acknowledge();
      log.info("✅ Event {} processed successfully", event.getEventId());
    } catch (Exception e) {
      log.error("❌ Error processing comment.created: {}", e.getMessage(), e);
      // KHÔNG acknowledge → Kafka sẽ retry
      // Sau N retries → gửi tới DLT (Dead Letter Topic)
      throw new RuntimeException(e);
    }
  }
}
