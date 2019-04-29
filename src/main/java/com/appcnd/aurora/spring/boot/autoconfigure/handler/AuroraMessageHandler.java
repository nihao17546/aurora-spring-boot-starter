package com.appcnd.aurora.spring.boot.autoconfigure.handler;

/**
 * @author nihao 2019/4/29
 */
public interface AuroraMessageHandler<T> {
    void onMessage(T message);
}
