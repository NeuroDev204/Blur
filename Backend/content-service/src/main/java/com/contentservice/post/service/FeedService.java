package com.contentservice.post.service;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class FeedService {
  PostFeedRepository postFeedRepository;

  @Cacheable(value = "feed", key = "#root.target.getCurrentUserId() + ':'+#page +':' + #size", unless = "#result.isEmpty()")
  public Page<PostFeedItem> getMyFeed(int page, int size) {
    String userId = getCurrentUserId();
    return postFeedRepository
        .findByTargetUserIdOrderByCreatedDateDesc(userId, PageRequest.of(page, size));
  }

  public String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
