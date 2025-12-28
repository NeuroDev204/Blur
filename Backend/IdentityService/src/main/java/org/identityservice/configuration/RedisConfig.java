package org.identityservice.configuration;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * ObjectMapper cho Redis - MongoDB compatible
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // CRITICAL: Handle unknown properties
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                // CRITICAL: Don't fail on missing creator properties
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .build();

        // Set visibility - only serialize fields, not getters
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // Activate default typing with LaissezFaire
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        log.info("Redis ObjectMapper configured for MongoDB entities");
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Custom serializer with error handling
        CustomRedisSerializer valueSerializer = new CustomRedisSerializer(createRedisObjectMapper());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        log.info("✅ RedisTemplate configured for MongoDB");
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        CustomRedisSerializer serializer = new CustomRedisSerializer(createRedisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("userById", defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("myInfo", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .transactionAware()
                .build();
    }

    /**
     * Custom Serializer with graceful error handling
     */
    static class CustomRedisSerializer implements org.springframework.data.redis.serializer.RedisSerializer<Object> {

        private final ObjectMapper objectMapper;

        public CustomRedisSerializer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public byte[] serialize(Object value) throws org.springframework.data.redis.serializer.SerializationException {
            if (value == null) {
                return new byte[0];
            }
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(value);
                log.debug("Serialized object of type: {}", value.getClass().getName());
                return bytes;
            } catch (Exception e) {
                log.error("❌ Serialize error for type {}: {}", value.getClass().getName(), e.getMessage(), e);
                // Don't cache if serialization fails
                throw new org.springframework.data.redis.serializer.SerializationException(
                        "Could not serialize: " + e.getMessage(), e);
            }
        }

        @Override
        public Object deserialize(byte[] bytes)
                throws org.springframework.data.redis.serializer.SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                Object result = objectMapper.readValue(bytes, Object.class);
                log.debug(
                        "Deserialized object of type: {}",
                        result != null ? result.getClass().getName() : "null");
                return result;
            } catch (Exception e) {
                log.error("❌ Deserialize error: {}", e.getMessage());
                // Return null on error - cache miss, will fetch from DB
                return null;
            }
        }
    }
}
