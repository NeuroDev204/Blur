package com.contentservice.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class FeignConfig {

    @Bean
    public ObjectMapper feignObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        // KHÔNG dùng activateDefaultTyping cho Feign
        return mapper;
    }

    @Bean
    public Decoder feignDecoder(ObjectMapper feignObjectMapper) {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(feignObjectMapper);
        ObjectFactory<HttpMessageConverters> objectFactory =
                () -> new HttpMessageConverters(converter);
        return new SpringDecoder(objectFactory);
    }

    @Bean
    public Encoder feignEncoder(ObjectMapper feignObjectMapper) {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(feignObjectMapper);
        ObjectFactory<HttpMessageConverters> objectFactory =
                () -> new HttpMessageConverters(converter);
        return new SpringEncoder(objectFactory);
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // Để debug
    }
}