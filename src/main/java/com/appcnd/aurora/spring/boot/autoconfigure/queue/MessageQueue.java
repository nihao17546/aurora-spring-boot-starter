package com.appcnd.aurora.spring.boot.autoconfigure.queue;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nihao 2019/4/29
 */
public class MessageQueue implements IntegrationQueue {
    private Map<String,MessageChannel> channelMap = new HashMap<String, MessageChannel>();

    public boolean send(String queueName, Serializable message) {
        MessageChannel channel = channelMap.get(queueName);
        if (channel == null) {
            throw new NullPointerException("queue[" + queueName + "] not exist");
        }
        return channel.send(MessageBuilder.withPayload(message).build());
    }

    public void addChannel(String queueName, MessageChannel messageChannel) {
        this.channelMap.put(queueName, messageChannel);
    }
}
