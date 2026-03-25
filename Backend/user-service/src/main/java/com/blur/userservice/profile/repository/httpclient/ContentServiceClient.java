package com.blur.userservice.profile.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "content-service", url = "${app.services.content:http://localhost:8082}")
public interface ContentServiceClient {

    @PostMapping("/internal/feed/backfill")
    void backfillFeed(
            @RequestParam("followerUserId") String followerUserId,
            @RequestParam("followedUserId") String followedUserId);
}
