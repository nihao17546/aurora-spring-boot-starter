package com.appcnd.aurora.spring.boot.autoconfigure.processor;

import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.QueueType;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.QueueModel;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.QueueProperties;
import com.appcnd.aurora.spring.boot.autoconfigure.queue.MessageQueue;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Destination;

/**
 * @author nihao 2019/4/29
 */
public class AuroraQueuePostProcessor implements BeanPostProcessor,ApplicationContextAware {
    private DefaultListableBeanFactory beanFactory;
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    @Autowired
    private QueueProperties queueProperties;

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.MYSQL_JDBC_CHANNEL_MESSAGE_STORE_BEAN_NAME)
    private JdbcChannelMessageStore jdbcChannelMessageStore;

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.RABBIT_TEMPLATE_BEAN_NAME)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.ACTIVE_JMS_TEMPLATE)
    private JmsTemplate jmsTemplate;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MessageQueue) {
            MessageQueue messageQueue = (MessageQueue) bean;
            for (QueueModel queue : queueProperties.getQueues()) {
                QueueChannel channel = null;
                if (QueueType.Mysql.name().equalsIgnoreCase(queue.getType())) {
                    channel = processMysql(queue);
                } else if (QueueType.RabbitMq.name().equalsIgnoreCase(queue.getType())) {
                    channel = processRabbit(queue);
                } else if (QueueType.ActiveMq.name().equalsIgnoreCase(queue.getType())) {
                    channel = processActive(queue);
                } else {
                    throw new IllegalArgumentException("queue type[" + queue.getType() + "] not exists");
                }
                messageQueue.addChannel(queue.getName(), channel);
            }
        }
        return bean;
    }

    private QueueChannel processMysql(QueueModel queue) {
        QueueChannel channel = MessageChannels.queue(jdbcChannelMessageStore, queue.getGroupId()).get();
        channel.afterPropertiesSet();
        beanFactory.registerSingleton(queue.getName() + "$Channel", channel);
        return channel;
    }

    private QueueChannel processRabbit(QueueModel queue) {
        String amqpOutboundEndpointName = queue.getName() + "$AmqpOutboundEndpoint";
        BeanDefinitionBuilder amqpOutboundEndpointBuilder = BeanDefinitionBuilder.genericBeanDefinition(AmqpOutboundEndpoint.class);
        amqpOutboundEndpointBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .addConstructorArgValue(rabbitTemplate)
                .addPropertyValue("exchangeName", queue.getExchangeName());
        if (queue.getRoutingKey() != null) {
            amqpOutboundEndpointBuilder.addPropertyValue("routingKey", queue.getRoutingKey());
        }
        beanFactory.registerBeanDefinition(amqpOutboundEndpointName, amqpOutboundEndpointBuilder.getRawBeanDefinition());
        AmqpOutboundEndpoint amqpOutboundEndpoint = beanFactory.getBean(amqpOutboundEndpointName, AmqpOutboundEndpoint.class);

        QueueChannel channel = MessageChannels.queue().get();
        channel.afterPropertiesSet();
        beanFactory.registerSingleton(queue.getName() + "$Channel", channel);

        String integrationFlowName = queue.getName() + "$IntegrationFlow";
        StandardIntegrationFlow integrationFlow = IntegrationFlows.from(channel).handle(amqpOutboundEndpoint).get();
        integrationFlow = (StandardIntegrationFlow) beanFactory.getBean(IntegrationFlowBeanPostProcessor.class)
                .postProcessBeforeInitialization(integrationFlow, integrationFlowName);
        beanFactory.registerSingleton(integrationFlowName, integrationFlow);

        return channel;
    }

    private QueueChannel processActive(QueueModel queue) {
        String jmsSendingMessageHandlerName = queue.getName() + "$JmsSendingMessageHandler";
        BeanDefinitionBuilder jmsSendingMessageHandlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(JmsSendingMessageHandler.class);
        Destination destination = null;
        if (queue.getTopic()) {
            destination = new ActiveMQTopic(queue.getDestinationName());
        } else {
            destination = new ActiveMQQueue(queue.getDestinationName());
        }
        jmsSendingMessageHandlerBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .addConstructorArgValue(jmsTemplate)
                .addPropertyValue("destination", destination);
        beanFactory.registerBeanDefinition(jmsSendingMessageHandlerName, jmsSendingMessageHandlerBuilder.getRawBeanDefinition());
        JmsSendingMessageHandler jmsSendingMessageHandler = beanFactory.getBean(jmsSendingMessageHandlerName, JmsSendingMessageHandler.class);

        QueueChannel channel = MessageChannels.queue().get();
        channel.afterPropertiesSet();
        beanFactory.registerSingleton(queue.getName() + "$Channel", channel);

        String integrationFlowName = queue.getName() + "$IntegrationFlow";
        StandardIntegrationFlow integrationFlow = IntegrationFlows.from(channel).handle(jmsSendingMessageHandler).get();
        integrationFlow = (StandardIntegrationFlow) beanFactory.getBean(IntegrationFlowBeanPostProcessor.class)
                .postProcessBeforeInitialization(integrationFlow, integrationFlowName);
        beanFactory.registerSingleton(integrationFlowName, integrationFlow);

        return channel;
    }
}
