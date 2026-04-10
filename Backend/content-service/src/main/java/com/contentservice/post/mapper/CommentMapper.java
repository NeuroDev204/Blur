package com.contentservice.post.mapper;

import com.contentservice.post.dto.response.CommentResponse;
import com.contentservice.post.entity.Comment;

import com.contentservice.post.entity.CommentReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentResponse toCommentResponse(Comment comment);

    @Mapping(source = "userName", target = "userName")
    CommentResponse toCommentResponse(CommentReply commentReply);
}
