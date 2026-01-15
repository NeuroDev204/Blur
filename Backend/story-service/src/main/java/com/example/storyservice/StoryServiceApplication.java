package com.example.storyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@ComponentScan(basePackages = {"com.example.storyservice", "com.blur.common"})

public class StoryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(StoryServiceApplication.class, args);
  }

}
