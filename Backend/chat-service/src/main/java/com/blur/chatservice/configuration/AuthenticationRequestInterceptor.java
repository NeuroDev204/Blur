package com.blur.chatservice.configuration;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class AuthenticationRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String authHeader = null;
        if (servletRequestAttributes != null) {
            authHeader = servletRequestAttributes.getRequest().getHeader("Authorization");
        }

        if (StringUtils.hasText(authHeader)) template.header("Authorization", authHeader);
    }
}
