package com.postservice.mapper;

import com.postservice.dto.response.CommentResponse;
import com.postservice.entity.Comment;

import com.postservice.entity.CommentReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentResponse toCommentResponse(Comment comment);

    @Mapping(source = "userName", target = "userName")
    @Mapping(target = "commentId", source = "commentId")  // ✅ MAP field này
    @Mapping(target = "parentReplyId", source = "parentReplyId")
    CommentResponse toCommentResponse(CommentReply commentReply);
}
