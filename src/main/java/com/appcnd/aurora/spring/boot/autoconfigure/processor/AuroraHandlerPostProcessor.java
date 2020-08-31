package com.appcnd.aurora.spring.boot.autoconfigure.processor;

import com.appcnd.aurora.spring.boot.autoconfigure.annotation.ActiveListener;
import com.appcnd.aurora.spring.boot.autoconfigure.annotation.MysqlListener;
import com.appcnd.aurora.spring.boot.autoconfigure.annotation.RabbitListener;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.handler.AuroraMessageHandler;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

import javax.jms.Destination;
import java.lang.annotation.Annotation;

/**
 * @author nihao 2019/4/29
 */
public class AuroraHandlerPostProcessor implements BeanPostProcessor,ApplicationContextAware {
    private DefaultListableBeanFactory beanFactory;
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.MYSQL_JDBC_CHANNEL_MESSAGE_STORE_BEAN_NAME)
    private JdbcChannelMessageStore jdbcChannelMessageStore;

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.RABBIT_CONNECTION_FACTORY_BEAN_NAME)
    private ConnectionFactory connectionFactory;

    @Autowired(required = false)
    @Qualifier(BeanNameConstant.ACTIVE_JMS_TEMPLATE)
    private JmsTemplate jmsTemplate;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AuroraMessageHandler) {
            AuroraMessageHandler handler = (AuroraMessageHandler) bean;
            MysqlListener mysqlListener = AnnotationUtils.findAnnotation(handler.getClass(), MysqlListener.class);
            if (mysqlListener != null) {
                processMysql(mysqlListener.groupId(), beanName, handler);
            }
            RabbitListener rabbitListener = AnnotationUtils.findAnnotation(handler.getClass(), RabbitListener.class);
            if (rabbitListener != null) {
                processRabbit(rabbitListener.queueName(), beanName, handler);
            }
            ActiveListener activeListener = AnnotationUtils.findAnnotation(handler.getClass(), ActiveListener.class);
            if (activeListener != null) {
                processActive(activeListener.destinationName(), activeListener.topic(), beanName, handler);
            }
        }
        return bean;
    }

    private void processMysql(String groupId, String beanName, final AuroraMessageHandler handler) {
        Assert.notNull(jdbcChannelMessageStore, "Int Mysql DataSource not exists");
        Assert.notNull(groupId, "groupId is required");

        String channelName = beanName + "$Channel";
        QueueChannel queueChannel = MessageChannels.queue(jdbcChannelMessageStore, groupId).get();
        queueChannel.afterPropertiesSet();
        beanFactory.registerSingleton(channelName, queueChannel);

        String integrationFlowName = beanName + "$IntegrationFlow";
        StandardIntegrationFlow integrationFlow = IntegrationFlows.from(queueChannel).handle(new MessageHandler() {
            public void handleMessage(Message<?> message) throws MessagingException {
                handler.onMessage(message.getPayload());
            }
        }).get();
        integrationFlow = (StandardIntegrationFlow) beanFactory.getBean(IntegrationFlowBeanPostProcessor.class)
                .postProcessBeforeInitialization(integrationFlow, integrationFlowName);
        beanFactory.registerSingleton(integrationFlowName, integrationFlow);
    }

    private void processRabbit(String queueName, String beanName, final AuroraMessageHandler handler) {
        Assert.notNull(connectionFactory, "Int RabbitConnectionFactory DataSource not exists");
        Assert.notNull(queueName, "queueName is required");

        String containerBeanName = beanName + "$SimpleMessageListenerContainer";
        BeanDefinitionBuilder containerBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);
        containerBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionFactory)
                .addPropertyValue("queueNames", queueName);
        beanFactory.registerBeanDefinition(containerBeanName, containerBuilder.getRawBeanDefinition());
        SimpleMessageListenerContainer container = beanFactory.getBean(containerBeanName, SimpleMessageListenerContainer.class);

        String adapterBeanName = beanName + "$AmqpInboundChannelAdapter";
        BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(AmqpInboundChannelAdapter.class);
        adapterBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .addConstructorArgValue(container);
        beanFactory.registerBeanDefinition(adapterBeanName, adapterBuilder.getRawBeanDefinition());
        AmqpInboundChannelAdapter adapter = beanFactory.getBean(adapterBeanName, AmqpInboundChannelAdapter.class);

        String integrationFlowName = beanName + "$IntegrationFlow";
        StandardIntegrationFlow integrationFlow = IntegrationFlows.from(adapter).handle(new MessageHandler() {
            public void handleMessage(Message<?> message) throws MessagingException {
                handler.onMessage(message.getPayload());
            }
        }).get();
        integrationFlow = (StandardIntegrationFlow) beanFactory.getBean(IntegrationFlowBeanPostProcessor.class)
                .postProcessBeforeInitialization(integrationFlow, integrationFlowName);
        beanFactory.registerSingleton(integrationFlowName, integrationFlow);
    }

    private void processActive(String destinationName, boolean topic, String beanName, final AuroraMessageHandler handler) {
        Assert.notNull(jmsTemplate, "Int Active JmsTemplate not exists");
        Assert.notNull(destinationName, "destinationName is required");

        String jmsSourceBeanName = beanName + "$JmsDestinationPollingSource";
        Destination destination = null;
        if (topic) {
            destination = new ActiveMQTopic(destinationName);
        } else {
            destination = new ActiveMQQueue(destinationName);
        }
        BeanDefinitionBuilder jmsSourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(JmsDestinationPollingSource.class);
        jmsSourceBuilder.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .addConstructorArgValue(jmsTemplate)
                .addPropertyValue("destination", destination);
        beanFactory.registerBeanDefinition(jmsSourceBeanName, jmsSourceBuilder.getRawBeanDefinition());
        JmsDestinationPollingSource jmsSource = beanFactory.getBean(jmsSourceBeanName, JmsDestinationPollingSource.class);

        String integrationFlowName = beanName + "$IntegrationFlow";
        StandardIntegrationFlow integrationFlow = IntegrationFlows.from(jmsSource).handle(new MessageHandler() {
            public void handleMessage(Message<?> message) throws MessagingException {
                handler.onMessage(message.getPayload());
            }
        }).get();
        integrationFlow = (StandardIntegrationFlow) beanFactory.getBean(IntegrationFlowBeanPostProcessor.class)
                .postProcessBeforeInitialization(integrationFlow, integrationFlowName);
        beanFactory.registerSingleton(integrationFlowName, integrationFlow);
    }
}
