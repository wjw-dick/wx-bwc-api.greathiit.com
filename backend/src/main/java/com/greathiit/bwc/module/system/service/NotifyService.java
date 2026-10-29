package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.module.system.entity.SysMessage;
import com.greathiit.bwc.module.system.entity.SysMessageLog;
import com.greathiit.bwc.module.system.entity.SysMessageTemplate;
import com.greathiit.bwc.module.system.mapper.SysMessageLogMapper;
import com.greathiit.bwc.module.system.mapper.SysMessageMapper;
import com.greathiit.bwc.module.system.mapper.SysMessageTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息兜底 + 消息发送日志。微信通道由 WeChatService 负责，失败时回落到站内消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final SysMessageMapper messageMapper;
    private final SysMessageLogMapper messageLogMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final MessageProducer messageProducer;

    public void send(Long tenantId, Long userId, String title, String content, String bizType, Long bizId) {
        SysMessage message = new SysMessage();
        message.setTenantId(tenantId);
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setReadFlag(0);
        messageMapper.insert(message);

        SysMessageLog log = new SysMessageLog();
        log.setTenantId(tenantId);
        log.setChannel("STATION");
        log.setBizType(bizType);
        log.setToUser(userId == null ? null : String.valueOf(userId));
        log.setContent(title);
        log.setStatus("SENT");
        messageLogMapper.insert(log);
        Map<String, Object> payload = new HashMap<>();
        payload.put("logId", log.getId());
        payload.put("tenantId", tenantId);
        payload.put("userId", userId);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("bizType", bizType);
        payload.put("bizId", bizId);
        messageProducer.publish(payload);
    }

    public void sendByTemplate(Long tenantId, Long userId, String bizType, String title, String content,
                               Long bizId) {
        send(tenantId, userId, title, content, bizType, bizId);
        List<SysMessageTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<SysMessageTemplate>()
                        .eq(SysMessageTemplate::getTenantId, tenantId)
                        .eq(SysMessageTemplate::getBizType, bizType)
                        .eq(SysMessageTemplate::getEnabled, 1));
        for (SysMessageTemplate template : templates) {
            SysMessageLog log = new SysMessageLog();
            log.setTenantId(tenantId);
            log.setChannel(template.getChannel());
            log.setBizType(bizType);
            log.setTemplateId(template.getTemplateId());
            log.setToUser(userId == null ? null : String.valueOf(userId));
            log.setContent(content);
            log.setStatus("QUEUED");
            messageLogMapper.insert(log);
            Map<String, Object> payload = new HashMap<>();
            payload.put("logId", log.getId());
            payload.put("tenantId", tenantId);
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("bizType", bizType);
            payload.put("bizId", bizId);
            payload.put("channel", template.getChannel());
            payload.put("templateId", template.getTemplateId());
            messageProducer.publish(payload);
        }
    }
}
