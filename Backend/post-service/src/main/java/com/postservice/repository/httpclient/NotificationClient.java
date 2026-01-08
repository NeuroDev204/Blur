package com.postservice.repository.httpclient;

import com.blur.common.dto.response.ApiResponse;
import com.postservice.configuration.AuthenticationRequestInterceptor;
import com.postservice.dto.event.Event;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${app.service.notification.url}",configuration = {AuthenticationRequestInterceptor.class})
public interface NotificationClient {
    @PutMapping("/like-post")
     ApiResponse<?> sendLikePostNotification(@RequestBody Event event);
    @PostMapping("/comment")
     ApiResponse<?> sendCommentNotification(@RequestBody Event event);
    @PostMapping("/reply-comment")
    ApiResponse<?> sendReplyCommentNotification(@RequestBody Event event);
}
