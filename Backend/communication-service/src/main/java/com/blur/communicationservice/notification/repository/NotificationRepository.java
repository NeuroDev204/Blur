package com.blur.communicationservice.notification.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.blur.communicationservice.notification.entity.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReceiverIdOrderByTimestampDesc(String receiverId);

    void deleteByReceiverId(String receiverId);

    List<Notification> findAllByReceiverId(String receiverId);
}
