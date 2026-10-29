package com.greathiit.bwc.common;

import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务编号生成：租户内唯一，Redis 自增 + 分布式锁语义；Redis 不可用时降级为本地序列。
 * 格式：{租户编码}-{前缀}-{yyyyMMdd}-{0001}
 */
@Component
@RequiredArgsConstructor
public class NoGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;
    private final SysTenantMapper tenantMapper;
    private final Map<Long, String> tenantCodes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localSeq = new ConcurrentHashMap<>();

    public String next(String prefix) {
        Long tenantId = com.greathiit.bwc.security.TenantContext.getTenantId();
        String code = tenantCodes.computeIfAbsent(tenantId, id -> {
            SysTenant tenant = tenantMapper.selectById(id);
            return tenant == null || tenant.getCode() == null ? "T" + id : tenant.getCode();
        });
        String day = LocalDate.now().format(DAY);
        String key = "bwc:no:" + tenantId + ":" + prefix + ":" + day;
        long seq;
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redisTemplate.expire(key, java.time.Duration.ofDays(2));
            }
            seq = value == null ? fallback(key) : value;
        } catch (Exception e) {
            seq = fallback(key);
        }
        return String.format("%s-%s-%s-%04d", code, prefix, day, seq);
    }

    private long fallback(String key) {
        return localSeq.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
}
