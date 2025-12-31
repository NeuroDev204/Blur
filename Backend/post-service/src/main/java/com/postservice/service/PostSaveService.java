package com.postservice.service;

import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.postservice.dto.response.PostResponse;
import com.postservice.entity.Post;
import com.postservice.entity.PostSave;
import com.postservice.mapper.PostMapper;
import com.postservice.repository.PostRepository;
import com.postservice.repository.PostSaveRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PostSaveService {


    PostRepository postRepository;
    PostSaveRepository postSaveRepository;
    PostMapper postMapper;


    @CacheEvict(value = "savedPosts", key = "#root.target.getCurrentUserId()")
    public String savePost(String postId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND));
        if(userId.equals(post.getUserId())) {
            throw new BlurException(ErrorCode.CANNOT_SAVE_YOUR_POST);
        }
        PostSave postSave = PostSave.builder()
                .postId(postId)
                .userId(userId)
                .savedAt(Instant.now())
                .build();
        postSaveRepository.save(postSave);
        return "Post saved";
    }

    @CacheEvict(value = "savedPosts", key = "#root.target.getCurrentUserId()")
    public String unsavePost(String postId){
        PostSave postSave = postSaveRepository.findByPostId(postId);
        postSaveRepository.delete(postSave);
        return "Post saved";

    }
    @Cacheable(
            value = "savedPosts",
            key = "#root.target.getCurrentUserId()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<PostResponse> getAllSavedPost(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        var listSavedPost = postSaveRepository.findAll()
                .stream()
                .filter(postSave -> postSave.getUserId().equals(userId))
                .toList();
        List<PostResponse> postResponses = new ArrayList<>();
        for(PostSave postSave : listSavedPost){
            Optional<Post> post = postRepository.findById(postSave.getPostId());

            if(post.isPresent()) {
                log.info("post {}" ,post.get());
                PostResponse postResponse = postMapper.toPostResponse(post.get());
                postResponses.add(postResponse);
            }

        }
        return postResponses;
    }
    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
