package com.blur.userservice.profile.resilience;

import org.springframework.stereotype.Service;

import com.blur.userservice.profile.repository.httpclient.ContentServiceClient;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientContentServiceClient {
  private final ContentServiceClient contentServiceClient;
  @Bulkhead(name = "contentServiceBH")
  @CircuitBreaker(name = "contentServiceCB", fallbackMethod = "backfillFeedFallback")
  @Retry(name = "contentServiceRetry")
  public void backfillFeed(Object request){
    // contentServiceClient.backfillFeed(request); 
  }
  private void backfillFeedFallback(Object request, Throwable t){
    log.warn("Content service khong kha dung cho feed backfill: {}", t.getMessage());
  }
}
