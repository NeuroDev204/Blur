package com.contentservice.post.controller;

import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.service.CommentReplyService;
import com.contentservice.post.service.CommentService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {
    CommentService commentService;
    CommentReplyService commentReplyService;

    @PostMapping("/{postId}/create")
    public ApiResponse<CommentResponse> createComment(@PathVariable String postId,
                                                      @RequestBody CreateCommentRequest comment) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentService.createComment(comment, postId))
                .build();
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getAllComments(@PathVariable String postId) {
        return ApiResponse.<List<CommentResponse>>builder()
                .result(commentService.getAllCommentByPostId(postId))
                .build();
    }

    @GetMapping("/{postId}/all-comments")
    public ApiResponse<List<CommentResponse>> getAllCommentsWithReplies(@PathVariable String postId) {
        log.info("🔵 [CONTROLLER] Getting all comments + replies for post: {}", postId);

        // 1. Lấy tất cả comments gốc
        List<CommentResponse> rootComments = commentService.getAllCommentByPostId(postId);
        log.info("   → Found {} root comments", rootComments.size());

        // 2. Lấy tất cả replies của từng comment
        List<CommentResponse> allReplies = new ArrayList<>();
        for (CommentResponse comment : rootComments) {
            List<CommentResponse> replies = commentReplyService.getAllCommentReplyByCommentId(comment.getId());
            allReplies.addAll(replies);
        }
        log.info("   → Found {} replies", allReplies.size());

        // 3. Merge lại: comments + replies
        List<CommentResponse> allComments = new ArrayList<>();
        allComments.addAll(rootComments);
        allComments.addAll(allReplies);

        log.info("✅ [CONTROLLER] Returning {} total comments (root + replies)", allComments.size());

        return ApiResponse.<List<CommentResponse>>builder()
                .result(allComments)
                .build();
    }

    @GetMapping("/{commentId}")
    public ApiResponse<CommentResponse> getCommentById(@PathVariable String commentId) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentService.getCommentById(commentId))
                .build();
    }

    @PutMapping("/{commentId}/update")
    public ApiResponse<CommentResponse> updateComment(@PathVariable String commentId,
                                                      @RequestBody CreateCommentRequest comment) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentService.updateComment(commentId, comment))
                .build();
    }

    @DeleteMapping("/{commentId}/delete")
    public ApiResponse<String> deleteComment(@PathVariable String commentId) {
        return ApiResponse.<String>builder()
                .result(commentService.deleteComment(commentId))
                .build();
    }


}
