package com.example.storyservice.repository.httpclient;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.event.Event;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${app.service.notification.url}")
public interface NotificationClient {
  @PostMapping("/like-story")
  public ApiResponse<?> sendLikeStoryNotification(@RequestBody Event event);
}
