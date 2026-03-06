package com.contentservice.configuration;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom key generators for optimized cache keys
 */
@Configuration
public class CacheKeyConfig {

    /**
     * Custom key generator for pagination
     * Format: page:limit:sortBy:sortDir
     */
    @Bean("paginationKeyGenerator")
    public KeyGenerator paginationKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            for (Object param : params) {
                sb.append(param).append(":");
            }
            return sb.toString();
        };
    }

    /**
     * Custom key generator for user-specific data
     * Format: userId:additionalParams
     */
    @Bean("userKeyGenerator")
    public KeyGenerator userKeyGenerator() {
        return (target, method, params) -> {
            if (params.length == 0) {
                return "default";
            }
            return String.valueOf(params[0]);
        };
    }
}