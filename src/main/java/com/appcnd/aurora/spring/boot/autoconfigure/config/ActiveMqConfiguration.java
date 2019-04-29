package com.appcnd.aurora.spring.boot.autoconfigure.config;

import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.ActiveProperties;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author nihao 2019/4/29
 */
@ConditionalOnProperty({"spring.aurora.activemq.broker-url"})
@EnableConfigurationProperties(ActiveProperties.class)
public class ActiveMqConfiguration {
    @Autowired
    private ActiveProperties activeProperties;

    @Autowired(required = false)
    @Qualifier(PollerMetadata.DEFAULT_POLLER)
    private PollerSpec spec;

    @Bean(name = BeanNameConstant.ACTIVE_CONNECTION_FACTORY)
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(activeProperties.getBrokerUrl());
        if (activeProperties.getPassword() != null) {
            activeMQConnectionFactory.setPassword(activeProperties.getPassword());
        }
        if (activeProperties.getUsername() != null) {
            activeMQConnectionFactory.setUserName(activeProperties.getUsername());
        }
        if (spec != null && activeProperties.getTransaction()) {
            spec.transactional(new JmsTransactionManager(activeMQConnectionFactory));
        }
        return activeMQConnectionFactory;
    }

    @Bean(name = BeanNameConstant.ACTIVE_JMS_TEMPLATE)
    public JmsTemplate jmsTemplate(@Autowired @Qualifier(BeanNameConstant.ACTIVE_CONNECTION_FACTORY)
                                               ActiveMQConnectionFactory activeMQConnectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactory);
        return jmsTemplate;
    }
}
