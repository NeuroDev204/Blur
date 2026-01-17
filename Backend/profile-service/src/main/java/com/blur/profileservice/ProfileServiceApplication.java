package com.blur.profileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.blur.profileservice", "com.blur.common"})
@EnableMongoRepositories(basePackages = {"com.blur.profileservice", "com.blur.common.repository"})
@EnableScheduling
public class ProfileServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProfileServiceApplication.class, args);
  }

}
