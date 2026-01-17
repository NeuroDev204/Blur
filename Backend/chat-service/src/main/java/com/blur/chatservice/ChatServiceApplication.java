package com.blur.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.blur.chatservice", "com.blur.common"})
@EnableMongoRepositories(basePackages = {"com.blur.chatservice", "com.blur.common.repository"})

public class ChatServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChatServiceApplication.class, args);
  }
}
