package com.greathiit.bwc.module.system.service;

import com.greathiit.bwc.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 通知消息生产者：RabbitMQ 不可用时仅记录日志，业务不阻塞。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, payload);
        } catch (Exception e) {
            log.warn("通知消息入队失败（已回落站内消息）: {}", e.getMessage());
        }
    }
}
