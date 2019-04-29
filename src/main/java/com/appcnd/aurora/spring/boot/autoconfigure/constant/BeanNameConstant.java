package com.appcnd.aurora.spring.boot.autoconfigure.constant;

/**
 * @author nihao 2019/4/29
 */
public interface BeanNameConstant {
    String MYSQL_DATA_SOURCE_BEAN_NAME = "Int$MysqlDataSource";
    String MYSQL_JDBC_CHANNEL_MESSAGE_STORE_BEAN_NAME = "Int$MysqlJdbcChannelMessageStore";
    String RABBIT_CONNECTION_FACTORY_BEAN_NAME = "Int$RabbitConnectionFactory";
    String RABBIT_TEMPLATE_BEAN_NAME = "Int$RabbitTemplate";
    String ACTIVE_CONNECTION_FACTORY = "Int$ActiveMQConnectionFactory";
    String ACTIVE_JMS_TEMPLATE = "Int$JmsTemplate";
}
