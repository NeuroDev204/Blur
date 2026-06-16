package com.blur.userservice.profile.repository.httpclient;

import com.blur.userservice.profile.dto.request.FollowNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${app.services.notification:http://localhost:8083}")
public interface NotificationServiceClient {

    @PostMapping("/chat/notification/internal/follow")
    void sendFollowNotification(@RequestBody FollowNotificationRequest request);
}
