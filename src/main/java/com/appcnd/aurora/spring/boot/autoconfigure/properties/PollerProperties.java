package com.appcnd.aurora.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author nihao 2019/4/29
 */
@ConfigurationProperties("spring.aurora.poller")
public class PollerProperties {
    private String type = "fixedDelay";
    private Long period = 1000L;
    private String expression = "*/1 * * * * ?";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
