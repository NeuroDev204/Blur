package com.blur.notificationservice.configuration;

import com.blur.common.configuration.CustomJwtDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
  private final CustomJwtDecoder customJwtDecoder;

  public JwtHandshakeInterceptor(final CustomJwtDecoder customJwtDecoder) {
    this.customJwtDecoder = customJwtDecoder;
  }

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    String uri = request.getURI().toString();
    if (uri.contains("/info")) {
      return true;
    }

    String token = null;
    if (request instanceof ServletServerHttpRequest servletRequest) {
      HttpServletRequest req = servletRequest.getServletRequest();
      String authHeader = req.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
      } else {
        token = req.getParameter("token");
      }
    }

    if (token == null) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    try {
      Jwt jwt = customJwtDecoder.decode(token);
      Authentication authentication = new JwtAuthenticationToken(jwt);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      String userId = authentication.getName();

      // ✅ Gắn Principal cho WebSocket session (rất quan trọng)
      Principal principal = () -> userId;
      attributes.put("user", principal); // <- CHÌA KHÓA
      attributes.put("userId", userId);

      log.info("✅ Handshake success for userId {}", userId);
      return true;
    } catch (JwtException e) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
  }


  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

  }
}
