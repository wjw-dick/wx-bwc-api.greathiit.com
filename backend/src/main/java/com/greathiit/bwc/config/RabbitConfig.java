package com.greathiit.bwc.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ：消息通知异步解耦（微信模板消息/订阅消息/短信），失败回落站内消息。
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "bwc.notify.exchange";
    public static final String QUEUE = "bwc.notify.queue";
    public static final String ROUTING_KEY = "bwc.notify";

    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notifyQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding notifyBinding(Queue notifyQueue, TopicExchange notifyExchange) {
        return BindingBuilder.bind(notifyQueue).to(notifyExchange).with(ROUTING_KEY);
    }
}
