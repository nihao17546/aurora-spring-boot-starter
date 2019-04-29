package com.appcnd.aurora.spring.boot.autoconfigure;

import com.appcnd.aurora.spring.boot.autoconfigure.config.ActiveMqConfiguration;
import com.appcnd.aurora.spring.boot.autoconfigure.config.MysqlConfiguration;
import com.appcnd.aurora.spring.boot.autoconfigure.config.RabbitMqConfiguration;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.PollerType;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.QueueType;
import com.appcnd.aurora.spring.boot.autoconfigure.processor.AuroraHandlerPostProcessor;
import com.appcnd.aurora.spring.boot.autoconfigure.processor.AuroraQueuePostProcessor;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.PollerProperties;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.QueueModel;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.QueueProperties;
import com.appcnd.aurora.spring.boot.autoconfigure.queue.IntegrationQueue;
import com.appcnd.aurora.spring.boot.autoconfigure.queue.MessageQueue;
import com.appcnd.aurora.spring.boot.autoconfigure.util.EnhanceAssert;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;

import javax.annotation.PostConstruct;

/**
 * @author nihao 2019/4/29
 */
@Configuration
@EnableConfigurationProperties({QueueProperties.class, PollerProperties.class})
@Import({MysqlConfiguration.class, RabbitMqConfiguration.class,
        ActiveMqConfiguration.class, AuroraHandlerPostProcessor.class,
        AuroraQueuePostProcessor.class})
public class AuroraAutoConfigure implements ApplicationContextAware {
    private DefaultListableBeanFactory beanFactory;
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    @Autowired
    private QueueProperties queueProperties;

    @Autowired
    private PollerProperties pollerProperties;

    @PostConstruct
    public void init() {
        if (PollerType.fixedDelay.name().equalsIgnoreCase(pollerProperties.getType())
                || PollerType.fixedRate.name().equalsIgnoreCase(pollerProperties.getType())) {
            EnhanceAssert.notNull(pollerProperties.getPeriod(), "poller period is required");
            if (pollerProperties.getPeriod() < 1) {
                throw new IllegalArgumentException("poller period can not be less than 1");
            }
        } else if (PollerType.cron.name().equalsIgnoreCase(pollerProperties.getType())) {
            EnhanceAssert.notNull(pollerProperties.getExpression(), "poller expression is required");
            String regEx = "(((^([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]))|^([0-9]|[0-5][0-9])|^(\\* ))((([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|([0-9]|[0-5][0-9]) |(\\* ))((([0-9]|[01][0-9]|2[0-3])(\\,|\\-|\\/){1}([0-9]|[01][0-9]|2[0-3]) )|([0-9]|[01][0-9]|2[0-3]) |(\\* ))((([0-9]|[0-2][0-9]|3[01])(\\,|\\-|\\/){1}([0-9]|[0-2][0-9]|3[01]) )|(([0-9]|[0-2][0-9]|3[01]) )|(\\? )|(\\* )|(([1-9]|[0-2][0-9]|3[01])L )|([1-7]W )|(LW )|([1-7]\\#[1-4] ))((([1-9]|0[1-9]|1[0-2])(\\,|\\-|\\/){1}([1-9]|0[1-9]|1[0-2]) )|([1-9]|0[1-9]|1[0-2]) |(\\* ))(([1-7](\\,|\\-|\\/){1}[1-7])|([1-7])|(\\?)|(\\*)|(([1-7]L)|([1-7]\\#[1-4]))))|(((^([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|^([0-9]|[0-5][0-9]) |^(\\* ))((([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|([0-9]|[0-5][0-9]) |(\\* ))((([0-9]|[01][0-9]|2[0-3])(\\,|\\-|\\/){1}([0-9]|[01][0-9]|2[0-3]) )|([0-9]|[01][0-9]|2[0-3]) |(\\* ))((([0-9]|[0-2][0-9]|3[01])(\\,|\\-|\\/){1}([0-9]|[0-2][0-9]|3[01]) )|(([0-9]|[0-2][0-9]|3[01]) )|(\\? )|(\\* )|(([1-9]|[0-2][0-9]|3[01])L )|([1-7]W )|(LW )|([1-7]\\#[1-4] ))((([1-9]|0[1-9]|1[0-2])(\\,|\\-|\\/){1}([1-9]|0[1-9]|1[0-2]) )|([1-9]|0[1-9]|1[0-2]) |(\\* ))(([1-7](\\,|\\-|\\/){1}[1-7] )|([1-7] )|(\\? )|(\\* )|(([1-7]L )|([1-7]\\#[1-4]) ))((19[789][0-9]|20[0-9][0-9])\\-(19[789][0-9]|20[0-9][0-9])))";
            if (!pollerProperties.getExpression().matches(regEx)) {
                throw new IllegalArgumentException("poller expression validate error");
            }
        } else {
            throw new IllegalArgumentException("poller type[" + pollerProperties.getType() + "] not exists");
        }
        for (QueueModel queue : queueProperties.getQueues()) {
            EnhanceAssert.notNull(queue.getName(), "name is required");
            EnhanceAssert.notNull(queue.getType(), "type is required");
            if (QueueType.Mysql.name().equalsIgnoreCase(queue.getType())) {
                EnhanceAssert.notNull(queue.getGroupId(), "groupId is required");
                if (!beanFactory.containsBean(BeanNameConstant.MYSQL_JDBC_CHANNEL_MESSAGE_STORE_BEAN_NAME)) {
                    throw new IllegalArgumentException("Int Mysql DataSource not exists");
                }
            } else if (QueueType.RabbitMq.name().equalsIgnoreCase(queue.getType())) {
                EnhanceAssert.notNull(queue.getExchangeName(), "exchangeName is required");
                if (!beanFactory.containsBean(BeanNameConstant.RABBIT_TEMPLATE_BEAN_NAME)) {
                    throw new IllegalArgumentException("Int RabbitTemplate not exists");
                }
            } else if (QueueType.ActiveMq.name().equalsIgnoreCase(queue.getType())) {
                EnhanceAssert.notNull(queue.getDestinationName(), "destinationName is required");
                if (!beanFactory.containsBean(BeanNameConstant.ACTIVE_CONNECTION_FACTORY)) {
                    throw new IllegalArgumentException("Int ActiveMQConnectionFactory not exists");
                }
            } else {
                throw new IllegalArgumentException("queue type[" + queue.getType() + "] not exists");
            }
        }
        if (EnhanceAssert.hasRepeat(queueProperties.getQueues())) {
            throw new IllegalArgumentException("duplicate queue name");
        }
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerSpec poller() {
        PollerSpec spec = null;
        if (pollerProperties.getType().equalsIgnoreCase(PollerType.fixedDelay.name())) {
            spec = Pollers.fixedDelay(pollerProperties.getPeriod());
        } else if (pollerProperties.getType().equalsIgnoreCase(PollerType.fixedRate.name())) {
            spec = Pollers.fixedRate(pollerProperties.getPeriod());
        } else if (pollerProperties.getType().equalsIgnoreCase(PollerType.cron.name())) {
            spec = Pollers.cron(pollerProperties.getExpression());
        } else {
            throw new IllegalArgumentException("poller type[" + pollerProperties.getType() + "] not exists");
        }
        return spec;
    }

    @Bean
    public IntegrationQueue integrationQueue() {
        IntegrationQueue integrationQueue = new MessageQueue();
        return integrationQueue;
    }
}
