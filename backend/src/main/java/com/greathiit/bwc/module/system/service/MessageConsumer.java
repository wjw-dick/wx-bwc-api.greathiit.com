package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.config.RabbitConfig;
import com.greathiit.bwc.module.system.entity.SysMessageLog;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysMessageLogMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知消费者：微信模板消息/订阅消息发送，失败标记 FAILED 并保留站内消息兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bwc.mq.listener-enabled", havingValue = "true", matchIfMissing = false)
public class MessageConsumer {

    private final SysMessageLogMapper messageLogMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final WeChatService weChatService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        Long tenantId = toLong(payload.get("tenantId"));
        Long logId = toLong(payload.get("logId"));
        String channel = payload.get("channel") == null ? "STATION" : String.valueOf(payload.get("channel"));
        if ("STATION".equalsIgnoreCase(channel)) {
            return;
        }
        TenantContext.set(tenantId, false, false, false);
        try {
            SysMessageLog logEntity = logId == null ? null : messageLogMapper.selectById(logId);
            Long userId = toLong(payload.get("userId"));
            String openid = resolveOpenid(userId);
            boolean sent = false;
            if (openid != null && payload.get("templateId") != null) {
                String templateId = String.valueOf(payload.get("templateId"));
                if ("MINI".equalsIgnoreCase(channel)) {
                    sent = weChatService.sendSubscribe(openid, templateId, subscribeData(payload),
                            payload.get("page") == null ? null : String.valueOf(payload.get("page")));
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("first", Map.of("value", payload.getOrDefault("title", "校园安全通知")));
                    data.put("keyword1", Map.of("value", payload.getOrDefault("content", "")));
                    data.put("remark", Map.of("value", "详情请登录校园安全平台查看"));
                    sent = weChatService.sendTemplate("MP", openid, templateId, data, null);
                }
            }
            if (logEntity != null) {
                logEntity.setStatus(sent ? "SENT" : "FALLBACK");
                if (!sent) {
                    logEntity.setError("微信通道未配置或发送失败，已回落站内消息");
                }
                messageLogMapper.updateById(logEntity);
            }
        } catch (Exception e) {
            log.warn("通知消费失败: {}", e.getMessage());
            if (logId != null) {
                SysMessageLog logEntity = messageLogMapper.selectById(logId);
                if (logEntity != null) {
                    logEntity.setStatus("FAILED");
                    logEntity.setError(e.getMessage());
                    messageLogMapper.updateById(logEntity);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    private Map<String, Object> subscribeData(Map<String, Object> payload) {
        Object provided = payload.get("data");
        if (provided instanceof Map<?, ?> map) {
            Map<String, Object> copy = new HashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return copy;
        }
        String title = String.valueOf(payload.getOrDefault("title", "校园安全通知"));
        String content = String.valueOf(payload.getOrDefault("content", "请及时查看"));
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", Map.of("value", truncate(title, 20)));
        data.put("thing2", Map.of("value", truncate(content, 20)));
        data.put("time3", Map.of("value", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        return data;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private String resolveOpenid(Long userId) {
        if (userId == null) {
            return null;
        }
        SysTenantUser user = tenantUserMapper.selectOne(new LambdaQueryWrapper<SysTenantUser>()
                .eq(SysTenantUser::getId, userId));
        return user == null ? null : user.getOpenid();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
