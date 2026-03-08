package com.blur.userservice.identity.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration is consolidated in RedisMultiDbConfig.
 * This class is kept for backward compatibility but all beans
 * are now defined in RedisMultiDbConfig with proper JSON serialization.
 */
@Configuration
public class RedisConfig {
}
