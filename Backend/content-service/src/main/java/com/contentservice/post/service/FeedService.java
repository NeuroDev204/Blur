package com.contentservice.post.service;

import com.contentservice.post.entity.Post;
import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class FeedService {
  PostFeedRepository postFeedRepository;
  PostRepository postRepository;
  ProfileClient profileClient;

  public Page<PostFeedItem> getMyFeed(int page, int size) {
    String userId = getCurrentUserId();
    return postFeedRepository
        .findByTargetUserIdOrderByCreatedDateDesc(userId, PageRequest.of(page - 1, size));
  }

  public void backfillFeedForFollower(String followerUserId, String followedUserId) {
    List<Post> recentPosts = postRepository.findAllByUserIdOrderByCreatedAtDesc(followedUserId);
    if (recentPosts.isEmpty()) {
      return;
    }

    String authorUsername = "";
    String authorFirstName = "";
    String authorLastName = "";
    String authorAvatar = "";
    try {
      var profileResponse = profileClient.getProfile(followedUserId);
      if (profileResponse != null && profileResponse.getResult() != null) {
        var profile = profileResponse.getResult();
        authorUsername = profile.getUsername() != null ? profile.getUsername() : "";
        authorFirstName = profile.getFirstName() != null ? profile.getFirstName() : "";
        authorLastName = profile.getLastName() != null ? profile.getLastName() : "";
        authorAvatar = profile.getImageUrl() != null ? profile.getImageUrl() : "";
      }
    } catch (Exception e) {
      log.warn("Failed to get profile for backfill: {}", e.getMessage());
    }

    int count = 0;
    for (Post post : recentPosts) {
      if (count >= 20) break;
      if (postFeedRepository.existsByPostIdAndTargetUserId(post.getId(), followerUserId)) {
        continue;
      }
      PostFeedItem item = PostFeedItem.builder()
          .postId(post.getId())
          .content(post.getContent())
          .imageUrls(post.getMediaUrls() != null ? post.getMediaUrls() : List.of())
          .videoUrl("")
          .authorId(followedUserId)
          .authorUsername(authorUsername)
          .authorFirstName(authorFirstName.isEmpty() && post.getFirstName() != null ? post.getFirstName() : authorFirstName)
          .authorLastName(authorLastName.isEmpty() && post.getLastName() != null ? post.getLastName() : authorLastName)
          .authorAvatar(authorAvatar)
          .targetUserId(followerUserId)
          .likeCount(0)
          .commentCount(0)
          .shareCount(0)
          .createdDate(LocalDateTime.ofInstant(post.getCreatedAt(), ZoneId.systemDefault()))
          .updatedDate(LocalDateTime.now())
          .build();
      postFeedRepository.save(item);
      count++;
    }
    log.info("Backfilled {} feed items for follower={} from author={}", count, followerUserId, followedUserId);
  }

  public String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
