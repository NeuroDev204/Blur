package com.blur.communicationservice.config;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blur.communicationservice.mapper.ConversationMapper;

@Configuration
public class MapperConfig {

    @Bean
    public ConversationMapper conversationMapper() {
        return Mappers.getMapper(ConversationMapper.class);
    }
}
