package com.appcnd.aurora.spring.boot.autoconfigure.properties;

import java.util.Arrays;

/**
 * @author nihao 2019/4/29
 */
public class QueueModel {
    private String name;
    private String type;

    // mysql
    private String groupId;

    // rabbitmq
    private String exchangeName;
    private String routingKey;

    // activemq
    private String destinationName;
    private Boolean topic = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Boolean getTopic() {
        return topic;
    }

    public void setTopic(Boolean topic) {
        this.topic = topic;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new String[]{this.getName()});
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QueueModel) {
            QueueModel oth = (QueueModel) obj;
            return this.getName() != null
                    && this.getName().equals(oth.getName());
        }
        return false;
    }
}
