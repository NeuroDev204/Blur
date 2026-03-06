package com.blur.communicationservice.notification.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.blur.communicationservice.exception.AppException;
import com.blur.communicationservice.exception.ErrorCode;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.repository.NotificationRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {
    NotificationRepository notificationRepository;

    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    public List<Notification> getForUser(String receiverId) {
        return notificationRepository.findByReceiverIdOrderByTimestampDesc(receiverId);
    }

    public String markAsRead(String notificationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();

        var notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiverId().equals(userId)) {
            throw new AppException(ErrorCode.YOU_ARE_NOT_ALLOWED);
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return "Marked as read";
    }

    public String markAllAsRead() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var notifications = notificationRepository.findAllByReceiverId(userId);
        notifications.forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
        return "Marked all as read";
    }
}
