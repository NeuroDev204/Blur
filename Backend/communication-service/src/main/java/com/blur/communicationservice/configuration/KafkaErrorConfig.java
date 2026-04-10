package com.blur.communicationservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KafkaErrorConfig {
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        var backOff = new FixedBackOff(1000L, 3L);
        var handler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error(
                            "Kafka message thất bại sau 3 lần retry. Topic={}, Partition={}, Offset={}, Error={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            exception.getMessage());
                },
                backOff);
        return handler;
    }
}
