package com.appcnd.aurora.spring.boot.autoconfigure.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.appcnd.aurora.spring.boot.autoconfigure.constant.BeanNameConstant;
import com.appcnd.aurora.spring.boot.autoconfigure.properties.MysqlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.MySqlChannelMessageStoreQueryProvider;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * @author nihao 2019/4/29
 */
@ConditionalOnProperty({"spring.aurora.mysql.url"})
@EnableConfigurationProperties(MysqlProperties.class)
public class MysqlConfiguration {
    @Autowired
    private MysqlProperties mysqlProperties;

    @Autowired(required = false)
    @Qualifier(PollerMetadata.DEFAULT_POLLER)
    private PollerSpec spec;

    private DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(mysqlProperties.getUrl());
        dataSource.setUsername(mysqlProperties.getUsername());
        dataSource.setPassword(mysqlProperties.getPassword());
        if (mysqlProperties.getInitialSize() != null) {
            dataSource.setInitialSize(mysqlProperties.getInitialSize());
        }
        if (mysqlProperties.getMaxActive() != null) {
            dataSource.setMaxActive(mysqlProperties.getMaxActive());
        }
        if (mysqlProperties.getMinIdle() != null) {
            dataSource.setMinIdle(mysqlProperties.getMinIdle());
        }
        if (mysqlProperties.getMaxWait() != null) {
            dataSource.setMaxWait(mysqlProperties.getMaxWait());
        }
        if (spec != null && mysqlProperties.getTransaction()) {
            spec.transactional(new DataSourceTransactionManager(dataSource));
        }
        return dataSource;
    }

    @Bean(name = BeanNameConstant.MYSQL_JDBC_CHANNEL_MESSAGE_STORE_BEAN_NAME)
    public JdbcChannelMessageStore jdbcChannelMessageStore() {
        JdbcChannelMessageStore jdbcChannelMessageStore = new JdbcChannelMessageStore(dataSource());
        jdbcChannelMessageStore.setChannelMessageStoreQueryProvider(new MySqlChannelMessageStoreQueryProvider());
        return jdbcChannelMessageStore;
    }
}
