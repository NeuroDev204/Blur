package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AtomicCounterService {
    private static final String INCREMENT_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            if current == false then
                redis.call('SET', KEYS[1], 1)
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                return 1
            else
                return redis.call('INCR', KEYS[1])
            end
            """;
    private static final String DECREMENT_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            if current == false or tonumber(current) <= 0 then
                return 0
            else
                return redis.call('DECR', KEYS[1])
            end
            """;
    private final RedisTemplate<String, Object> redisTemplate;

    public Long incrementCounter(String key, long ttlSeconds) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class);
            return redisTemplate.execute(script, Collections.singletonList(key), ttlSeconds);
        } catch (Exception e) {
            log.warn("Failed to increment counter key={}: {}", key, e.getMessage());
            return 0L;
        }
    }

    public Long decrementCounter(String key) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class);
            return redisTemplate.execute(script, Collections.singletonList(key));
        } catch (Exception e) {
            log.warn("Failed to decrement counter key={}: {}", key, e.getMessage());
            return 0L;
        }
    }

    public Long getCounter(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0L;

        } catch (Exception e) {
            log.warn("Failed to get counter key ={}: {}", key, e.getMessage());
            return 0L;
        }
    }
}
