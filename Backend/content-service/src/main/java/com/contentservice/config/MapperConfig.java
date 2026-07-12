package com.contentservice.config;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.contentservice.post.mapper.PostMapper;

@Configuration
public class MapperConfig {

    @Bean
    public PostMapper postMapper() {
        return Mappers.getMapper(PostMapper.class);
    }
}
