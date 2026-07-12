package com.blur.userservice.config;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blur.userservice.profile.mapper.UserProfileMapper;

@Configuration
public class MapperConfig {

    @Bean
    public UserProfileMapper userProfileMapper() {
        return Mappers.getMapper(UserProfileMapper.class);
    }
}
