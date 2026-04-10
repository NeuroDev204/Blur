package com.blur.apigateway.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/fallback")
public class FallbackController {
  @GetMapping("/user-service")
  public Mono<ResponseEntity<Map<String,Object>>> userServiceFallbackGet(){
    return buildFallbackResponse("user-service", "User Service đang tạm thời không khả dụng. Vui lòng thử lại sau.");
  }
  @PostMapping("/user-service")
  public Mono<ResponseEntity<Map<String,Object>>> userServiceFallbackPost(){
    return buildFallbackResponse("user-service", "User Service đang tạm thời không khả dụng. Vui lòng thử lại sau.");
  }
  @GetMapping("/content-service")
  public Mono<ResponseEntity<Map<String,Object>>> contentServiceFallbackGet(){
    return buildFallbackResponse("content-service", "Content Service đang tạm thời không khả dụng. Vui lòng thử lại sau");
  }
  @PostMapping("/content-service")
  public Mono<ResponseEntity<Map<String,Object>>> contentServiceFallbackPost(){
    return buildFallbackResponse("content-service","Content Service đang tạm thời không khả dụng. Vui lòng thử lại sau");
  }
  @GetMapping("/communication-service")
  public Mono<ResponseEntity<Map<String,Object>>> communicationServiceFallbackGet(){
    return buildFallbackResponse("communication-service", "Communication Service đang tạm thời không khả đụng vui lòng thử lại sau.");
  }
  @PostMapping("/communication-service")
  public Mono<ResponseEntity<Map<String,Object>>> communicationServiceFallbackPost(){
    return buildFallbackResponse("communication-service", "Communication Service đang tạm thời không khả đụng vui lòng thử lại sau.");
  }
  private Mono<ResponseEntity<Map<String,Object>>> buildFallbackResponse(String service, String message){
    return Mono.just(ResponseEntity
      .status(HttpStatus.SERVICE_UNAVAILABLE)
      .body(Map.of(
        "code", 503,
        "message", message, 
        "service", service
      )));
  }
}
