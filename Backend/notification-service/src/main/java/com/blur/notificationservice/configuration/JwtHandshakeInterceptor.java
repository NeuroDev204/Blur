package com.blur.notificationservice.configuration;

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
            
            // 1. ⭐ Thử lấy token từ Cookie trước (ưu tiên cao nhất)
            jakarta.servlet.http.Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        log.info("🍪 Token found in cookie");
                        break;
                    }
                }
            }
            
            // 2. Fallback: lấy từ Authorization header
            if (token == null) {
                String authHeader = req.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                    log.info("🔑 Token found in Authorization header");
                }
            }
            
            // 3. Fallback: lấy từ query parameter
            if (token == null) {
                token = req.getParameter("token");
                if (token != null) {
                    log.info("🔗 Token found in query parameter");
                }
            }
        }

        if (token == null) {
            log.warn("❌ No token found in cookie, header, or query param");
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
