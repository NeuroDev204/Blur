package com.contentservice.post.service;

import com.contentservice.configuration.AtomicCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentLockService {

    private static final String FLAG_COUNT_KEY_PREFIX = "comment:flag-count:";
    private static final String LOCK_KEY_PREFIX = "comment:lock:";

    /** Cửa sổ đếm số lần bị flag: 5 phút. */
    private static final long FLAG_WINDOW_SECONDS = 5 * 60;
    /** Thời gian khóa bình luận khi vượt ngưỡng: 10 phút. */
    public static final long LOCK_DURATION_SECONDS = 10 * 60;
    /**
     * Số lần FLAGGED tối đa trong cửa sổ; khóa khi vượt QUÁ ngưỡng này (lần thứ
     * 11).
     */
    private static final long FLAG_THRESHOLD = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final AtomicCounterService atomicCounterService;

    /**
     * Ghi nhận một bình luận vừa bị FLAGGED cho user. Nếu lần này khiến user vượt
     * ngưỡng
     * thì khóa user và trả về mốc thời gian (epoch millis) hết khóa; ngược lại trả
     * về 0.
     */
    public long registerFlag(String userId) {
        if (userId == null) {
            return 0L;
        }
        // Cửa sổ cố định 5 phút tính từ lần flag đầu tiên (TTL chỉ set khi tạo key).
        Long count = atomicCounterService.incrementCounter(FLAG_COUNT_KEY_PREFIX + userId, FLAG_WINDOW_SECONDS);
        if (count != null && count > FLAG_THRESHOLD && !isLocked(userId)) {
            return lockUser(userId);
        }
        return 0L;
    }

    /**
     * Khóa bình luận của user trong {@link #LOCK_DURATION_SECONDS} giây. Trả về mốc
     * hết khóa (epoch millis).
     */
    public long lockUser(String userId) {
        long lockedUntil = System.currentTimeMillis() + LOCK_DURATION_SECONDS * 1000L;
        try {
            redisTemplate.opsForValue().set(
                    LOCK_KEY_PREFIX + userId, lockedUntil, Duration.ofSeconds(LOCK_DURATION_SECONDS));
            // Reset bộ đếm để sau khi hết khóa user bắt đầu lại từ đầu.
            redisTemplate.delete(FLAG_COUNT_KEY_PREFIX + userId);
            log.info("Locked comments for user={} until={}", userId, lockedUntil);
        } catch (Exception e) {
            log.warn("Failed to lock comments for user={}: {}", userId, e.getMessage());
        }
        return lockedUntil;
    }

    /** Kiểm tra user có đang bị khóa bình luận không (fail-open nếu Redis lỗi). */
    public boolean isLocked(String userId) {
        if (userId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + userId));
        } catch (Exception e) {
            log.warn("Failed to check comment lock for user={}: {}", userId, e.getMessage());
            return false;
        }
    }
}
