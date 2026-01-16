package com.blur.apigateway.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CookieToHeaderFilter implements GlobalFilter, Ordered {
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    HttpCookie cookie = exchange.getRequest().getCookies().getFirst("access_token");
    if (cookie != null) {
      ServerHttpRequest mutatedRequest = (ServerHttpRequest) exchange.getRequest().mutate()
          .header("Authorization", "Bearer " + cookie.getValue())
          .build();

      return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return -100; // chay truoc authenticationfilter
  }
}
