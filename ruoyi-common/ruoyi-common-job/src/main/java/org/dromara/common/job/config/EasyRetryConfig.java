package org.dromara.common.job.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.collection.CollUtil;
import com.aizuda.easy.retry.client.common.appender.EasyRetryLogbackAppender;
import com.aizuda.easy.retry.client.common.event.ChannelReconnectEvent;
import com.aizuda.easy.retry.client.common.event.EasyRetryStartingEvent;
import com.aizuda.easy.retry.client.starter.EnableEasyRetry;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.job.config.properties.EasyRetryServerProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * 启动定时任务
 *
 * @author dhb52
 * @since 2024/3/12
 */
@AutoConfiguration
@EnableConfigurationProperties(EasyRetryServerProperties.class)
@ConditionalOnProperty(prefix = "easy-retry", name = "enabled", havingValue = "true")
@EnableScheduling
@EnableEasyRetry(group = "${easy-retry.group-name}")
public class EasyRetryConfig {

    @Autowired
    private EasyRetryServerProperties properties;
    @Autowired
    private DiscoveryClient discoveryClient;

    @EventListener(EasyRetryStartingEvent.class)
    public void onStarting(EasyRetryStartingEvent event) {
        // 从 nacos 获取 server 服务连接
        registerServer();
        // 注册 日志监控配置
        registerLogging();
    }

    @EventListener(ChannelReconnectEvent.class)
    public void onReconnect(ChannelReconnectEvent event) {
        // 连接中断 重新从 nacos 获取存活的服务连接(高可用配置)
        registerServer();
    }

    private void registerServer() {
        String serverName = properties.getServerName();
        if (StringUtils.isNotBlank(serverName)) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serverName);
            if (CollUtil.isNotEmpty(instances)) {
                ServiceInstance instance = instances.get(0);
                System.setProperty("easy-retry.server.host", instance.getHost());
                System.setProperty("easy-retry.server.port", properties.getPort());
            }
        }
    }

    private void registerLogging() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        EasyRetryLogbackAppender<ILoggingEvent> ca = new EasyRetryLogbackAppender<>();
        ca.setName("easy_log_appender");
        ca.start();
        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(ca);
    }

}