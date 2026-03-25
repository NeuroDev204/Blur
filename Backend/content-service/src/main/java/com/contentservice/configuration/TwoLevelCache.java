package com.contentservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

@Slf4j
public class TwoLevelCache implements Cache {
    private final String name;
    private final Cache l1Cache;
    private final Cache l2Cache;

    public TwoLevelCache(String name, Cache l1Cache, Cache l2Cache) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l2Cache.getNativeCache();
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
            log.debug("L1 HIT {}::{}", name, key);
            return l1Value;
        }
        ValueWrapper l2Value = l2Cache.get(key);
        if (l2Value != null) {
            log.debug("L2 HIT {}::{}", name, key);
            l1Cache.put(key, l2Value.get());
            return l2Value;
        }
        log.debug("MISS {}::{}", name, key);
        return null;
    }

    @Override
    public @Nullable <T> T get(Object key, @Nullable Class<T> type) {
        T l1Value = l1Cache.get(key, type);
        if (l1Value != null) {
            log.debug("L1 HIT {}::{}", name, key);
            return l1Value;
        }
        T l2Value = l2Cache.get(key, type);
        if (l2Value != null) {
            log.debug("L2 HIT {}::{}", name, key);
            l1Cache.put(key, l2Value);
            return l2Value;
        }
        log.debug("MISS {}::{}", name, key);
        return null;
    }

    @Override
    public @Nullable <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
            log.debug("L1 HIT {}::{}", name, key);
            @SuppressWarnings("unchecked")
            T result = (T) l1Value.get();
            return result;
        }
        T value = l2Cache.get(key, valueLoader);
        if (value != null) {
            log.debug("L2 HIT {}::{}", name, key);
            l1Cache.put(key, value);
        } else {
            log.debug("MISS {}::{}", name, key);
        }
        return value;
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        l2Cache.put(key, value);
        l1Cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l2Cache.evict(key);
        l1Cache.evict(key);
    }

    @Override
    public void clear() {
        l2Cache.clear();
        l1Cache.clear();
    }
}
