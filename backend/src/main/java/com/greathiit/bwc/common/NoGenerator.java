package com.greathiit.bwc.common;

import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class NoGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 前缀 -> 业务表/编号列，用于 Redis 计数丢失后按库内最大值续号，避免重号 */
    private static final Map<String, String[]> SEQ_SOURCES = Map.ofEntries(
            Map.entry("SE", new String[]{"bwc_security_event", "event_no"}),
            Map.entry("HZ", new String[]{"bwc_hazard", "hazard_no"}),
            Map.entry("DR", new String[]{"bwc_device_repair", "repair_no"}),
            Map.entry("PASS", new String[]{"bwc_visitor_appointment", "pass_code"}),
            Map.entry("LOST", new String[]{"bwc_lost_item", "item_no"}),
            Map.entry("LF", new String[]{"bwc_lost_item", "item_no"}),
            Map.entry("WO", new String[]{"bwc_work_order", "order_no"}),
            Map.entry("RA", new String[]{"bwc_report_apply", "report_no"}),
            Map.entry("RCPT", new String[]{"bwc_report_apply", "receipt_no"}),
            Map.entry("MA", new String[]{"bwc_monitor_access", "apply_no"}),
            Map.entry("FF", new String[]{"bwc_fire_facility", "facility_no"})
    );

    private final StringRedisTemplate redisTemplate;
    private final SysTenantMapper tenantMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Map<Long, String> tenantCodes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localSeq = new ConcurrentHashMap<>();

    public synchronized String next(String prefix) {
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
            if (value == null) {
                seq = dbMax(tenantId, code, prefix, day) + fallback(key);
            } else {
                long dbMax = dbMax(tenantId, code, prefix, day);
                if (value <= dbMax) {
                    // Redis 计数被清空/过期后，从数据库已有最大号继续，避免业务编号重复
                    seq = dbMax + 1;
                    redisTemplate.opsForValue().set(key, String.valueOf(seq));
                } else {
                    seq = value;
                }
                redisTemplate.expire(key, java.time.Duration.ofDays(2));
            }
        } catch (Exception e) {
            // Redis 不可用时也必须基于库内最大号续号，避免重启/断连后重号
            seq = dbMax(tenantId, code, prefix, day) + fallback(key);
        }
        return String.format("%s-%s-%s-%04d", code, prefix, day, seq);
    }

    private long dbMax(Long tenantId, String code, String prefix, String day) {
        String[] source = SEQ_SOURCES.get(prefix);
        if (source == null) {
            return 0;
        }
        String pattern = code + "-" + prefix + "-" + day + "-%";
        try {
            Long max = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(SUBSTRING_INDEX(" + source[1] + ", '-', -1) AS UNSIGNED)) FROM "
                            + source[0] + " WHERE tenant_id = ? AND " + source[1] + " LIKE ?",
                    Long.class, tenantId, pattern);
            return max == null ? 0 : max;
        } catch (Exception e) {
            log.warn("业务编号续号查询失败 prefix={}: {}", prefix, e.getMessage());
            return 0;
        }
    }

    private long fallback(String key) {
        return localSeq.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
}
