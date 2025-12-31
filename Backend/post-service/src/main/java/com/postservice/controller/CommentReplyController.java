package com.postservice.controller;

import com.blur.common.dto.response.ApiResponse;
import com.postservice.dto.request.CreateCommentRequest;
import com.postservice.dto.response.CommentResponse;
import com.postservice.service.CommentReplyService;
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


        CommentResponse result = commentReplyService.createCommentReply(commentId, parentReplyId, comment);


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