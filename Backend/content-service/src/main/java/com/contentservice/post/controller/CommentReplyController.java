package com.contentservice.post.controller;

import com.contentservice.post.dto.request.CreateCommentRequest;
import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.service.CommentReplyService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentReplyController {
    CommentReplyService commentReplyService;

    @PostMapping("/{commentId}/reply")
    public ApiResponse<CommentResponse> createCommentReply(
            @PathVariable String commentId,
            @RequestParam(required = false) String parentReplyId,
            @RequestBody CreateCommentRequest comment) {

        log.info("🟢🟢🟢 [CONTROLLER] ========== CREATE REPLY REQUEST ==========");
        log.info("🟢 [CONTROLLER] Comment ID: {}", commentId);
        log.info("🟢 [CONTROLLER] Parent Reply ID: {}", parentReplyId);
        log.info("🟢 [CONTROLLER] Content: {}", comment.getContent());
        log.info("🟢🟢🟢 [CONTROLLER] Calling CommentReplyService...");

        CommentResponse result = commentReplyService.createCommentReply(commentId, parentReplyId, comment);

        log.info("🟢🟢🟢 [CONTROLLER] Service returned successfully, reply ID: {}", result.getId());

        return ApiResponse.<CommentResponse>builder()
                .result(result)
                .build();
    }

    @PutMapping("/{commentReplyId}/updateReply")
    public ApiResponse<CommentResponse> updateCommentReply(
            @PathVariable String commentReplyId,
            @RequestBody CreateCommentRequest commentReply) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentReplyService.updateCommentReply(commentReplyId, commentReply))
                .build();
    }

    @GetMapping("/{commentId}/all")
    public ApiResponse<List<CommentResponse>> getAllCommentReplies(@PathVariable String commentId) {
        return ApiResponse.<List<CommentResponse>>builder()
                .result(commentReplyService.getAllCommentReplyByCommentId(commentId))
                .build();
    }

    @DeleteMapping("/{commentReplyId}/deleteReply")
    public ApiResponse<String> deleteCommentReply(@PathVariable String commentReplyId) {
        return ApiResponse.<String>builder()
                .result(commentReplyService.deleteCommentReply(commentReplyId))
                .build();
    }

    @GetMapping("/commentReply/{commentReplyId}")
    public ApiResponse<CommentResponse> getCommentReply(@PathVariable String commentReplyId) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentReplyService.getCommentReplyByCommentReplyId(commentReplyId))
                .build();
    }

    @GetMapping("/reply/{parentReplyId}/children")
    public ApiResponse<List<CommentResponse>> getRepliesByParentReplyId(@PathVariable String parentReplyId) {
        return ApiResponse.<List<CommentResponse>>builder()
                .result(commentReplyService.getRepliesByParentReplyId(parentReplyId))
                .build();
    }
}