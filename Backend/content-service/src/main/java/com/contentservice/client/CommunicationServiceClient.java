package com.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "communication-service", url = "${app.services.communication:http://localhost:8083/chat}")
public interface CommunicationServiceClient {

  @PostMapping("/notification/moderation-update")
  Map<String, Object> sendModerationUpdate(@RequestBody Map<String, String> request);
}
