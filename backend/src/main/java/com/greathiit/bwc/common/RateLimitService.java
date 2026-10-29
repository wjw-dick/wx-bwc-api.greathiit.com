package com.greathiit.bwc.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流与防重：Redis 原子自增/SetNX，Redis 不可用时降级到内存。
 */
@Component
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Long> localCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> localLocks = new ConcurrentHashMap<>();

    /**
     * 滑动窗口（固定窗口）限流：单位时间最多 limit 次。
     */
    public void check(String key, int limit, Duration window) {
        long count;
        try {
            Long value = redisTemplate.opsForValue().increment("bwc:rate:" + key);
            if (value != null && value == 1L) {
                redisTemplate.expire("bwc:rate:" + key, window);
            }
            count = value == null ? 0 : value;
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            long windowStart = now - window.toMillis();
            localCounters.entrySet().removeIf(en -> en.getValue() < windowStart);
            count = localCounters.merge(key, 1L, Long::sum);
        }
        if (count > limit) {
            throw BizException.tooMany("操作过于频繁，请稍后再试");
        }
    }

    /**
     * 防重复提交：同一 key 在 ttl 内只允许一次。
     */
    public void dedup(String key, Duration ttl) {
        boolean ok;
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent("bwc:dedup:" + key, "1", ttl);
            ok = Boolean.TRUE.equals(result);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            localLocks.entrySet().removeIf(en -> en.getValue() < now);
            ok = localLocks.putIfAbsent(key, now + ttl.toMillis()) == null;
        }
        if (!ok) {
            throw BizException.tooMany("请勿重复提交");
        }
    }
}
