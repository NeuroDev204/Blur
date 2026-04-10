package com.contentservice.post.service;

import com.contentservice.post.dto.response.PostResponse;
import com.contentservice.post.entity.Post;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.mapper.PostMapper;
import com.contentservice.post.repository.PostRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PostSaveService {

    PostRepository postRepository;
    PostMapper postMapper;

    @CacheEvict(value = "savedPosts", key = "#root.target.getCurrentUserId()")
    public String savePost(String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (userId.equals(post.getUserId())) {
            throw new AppException(ErrorCode.CANNOT_SAVE_YOUR_POST);
        }
        // Graph: (user_profile)-[:SAVED_POST {savedAt}]->(Post)
        postRepository.savePost(userId, postId, Instant.now());
        return "Post saved";
    }

    @CacheEvict(value = "savedPosts", key = "#root.target.getCurrentUserId()")
    public String unsavePost(String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        // Remove the (user_profile)-[:SAVED_POST]->(Post) edge
        postRepository.unsavePost(userId, postId);
        return "Post unsaved";
    }

    @Cacheable(value = "savedPosts", key = "#root.target.getCurrentUserId()", sync = true)
    public List<PostResponse> getAllSavedPost() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        // Single graph query: traverse (user_profile)-[:SAVED_POST]->(Post) — no N+1
        List<Post> savedPosts = postRepository.findSavedPostsByUserId(userId);
        return savedPosts.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
