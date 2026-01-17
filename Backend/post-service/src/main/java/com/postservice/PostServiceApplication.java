package com.postservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@ComponentScan(basePackages = { "com.postservice", "com.blur.common" })
@EnableMongoRepositories(basePackages = { "com.postservice", "com.blur.common.repository" })

public class PostServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PostServiceApplication.class, args);
  }

}
