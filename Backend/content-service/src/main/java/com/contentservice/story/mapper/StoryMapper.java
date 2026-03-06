package com.contentservice.story.mapper;


import com.contentservice.story.dto.request.CreateStoryRequest;
import com.contentservice.story.entity.Story;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface StoryMapper {
    Story toEntity(CreateStoryRequest createStoryRequest);
}
