package com.contentservice.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j

public class TwoLevelCacheManager implements CacheManager {
    private static final int L1_MAX_SIZE = 500;
    private static final Duration L1_TTL = Duration.ofSeconds(30);
    private final RedisCacheManager redisCacheManager;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(RedisCacheManager redisCacheManager) {
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public @Nullable Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n -> {
            Cache redisCache = redisCacheManager.getCache(n);
            if (redisCache == null) {
                return null;
            }
            CaffeineCache caffeineCache = new CaffeineCache(n,
                    Caffeine.newBuilder()
                            .maximumSize(L1_MAX_SIZE)
                            .expireAfterWrite(L1_TTL)
                            .recordStats()
                            .build());
            log.info("Created TwoLevelCache for: {}", n);
            return new TwoLevelCache(n, caffeineCache, redisCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisCacheManager.getCacheNames();
    }
}
