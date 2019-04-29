package com.appcnd.aurora.spring.boot.autoconfigure.queue;

import java.io.Serializable;

/**
 * @author nihao 2019/4/29
 */
public interface IntegrationQueue {
    boolean send(String queueName, Serializable message);
}
