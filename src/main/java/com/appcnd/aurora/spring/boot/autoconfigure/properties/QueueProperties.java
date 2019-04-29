package com.appcnd.aurora.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nihao 2019/4/29
 */
@ConfigurationProperties(prefix = "spring.aurora")
public class QueueProperties {
    private List<QueueModel> queues = new ArrayList<QueueModel>();

    public List<QueueModel> getQueues() {
        return queues;
    }

    public void setQueues(List<QueueModel> queues) {
        this.queues = queues;
    }
}
