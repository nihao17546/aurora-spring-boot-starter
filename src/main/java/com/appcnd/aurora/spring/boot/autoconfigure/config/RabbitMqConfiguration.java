package com.appcnd.aurora.spring.boot.autoconfigure.config;

import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.RabbitProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.scheduling.PollerMetadata;

/**
 * @author nihao 2019/4/29
 */
@ConditionalOnProperty({"spring.aurora.rabbitmq.host"})
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMqConfiguration {
    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired(required = false)
    @Qualifier(PollerMetadata.DEFAULT_POLLER)
    private PollerSpec spec;

    @Bean(name = BeanNameConstant.RABBIT_CONNECTION_FACTORY_BEAN_NAME)
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitProperties.getHost(), rabbitProperties.getPort());
        if (rabbitProperties.getUsername() != null) {
            connectionFactory.setUsername(rabbitProperties.getUsername());
        }
        if (rabbitProperties.getPassword() != null) {
            connectionFactory.setPassword(rabbitProperties.getPassword());
        }
        if (rabbitProperties.getVirtualHost() != null) {
            connectionFactory.setVirtualHost(rabbitProperties.getVirtualHost());
        }
        if (spec != null && rabbitProperties.getTransaction()) {
            spec.transactional(new RabbitTransactionManager(connectionFactory));
        }
        return connectionFactory;
    }

    @Bean(name = BeanNameConstant.RABBIT_TEMPLATE_BEAN_NAME)
    public RabbitTemplate rabbitTemplate(@Autowired @Qualifier(BeanNameConstant.RABBIT_CONNECTION_FACTORY_BEAN_NAME)
                                                     ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        return template;
    }
}
