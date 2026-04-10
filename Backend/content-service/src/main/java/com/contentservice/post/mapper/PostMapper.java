package com.contentservice.post.mapper;


import com.contentservice.post.dto.response.PostResponse;
import com.contentservice.post.entity.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostResponse toPostResponse(Post post);
}
