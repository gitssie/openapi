package com.gitssie.openapi.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.time.Duration;
import java.util.concurrent.locks.Lock;

/**
 * 根据Redis锁获的获取,控制单台节点执行定时任务
 */
public class QuartzStartup implements ApplicationListener<ApplicationReadyEvent>, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzStartup.class);
    private SchedulerFactoryBean quartz;
    private RedisLockRegistry lockRegistry;
    private ObjectProvider<RedisConnectionFactory> connectionFactory;
    private TaskScheduler taskScheduler;
    private Lock quartzLock;

    public QuartzStartup(SchedulerFactoryBean quartz, ObjectProvider<RedisConnectionFactory> connectionFactory) {
        this.quartz = quartz;
        this.connectionFactory = connectionFactory;
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.initialize();
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        lockRegistry = new RedisLockRegistry(connectionFactory.getIfAvailable(), "QuartzLock", Duration.ofMinutes(5).toMillis());
        quartzLock = lockRegistry.obtain(QuartzStartup.class.getSimpleName());
        taskScheduler.scheduleAtFixedRate(this, Duration.ofMinutes(1));
        LOGGER.info("start quartz lock schedule");
    }

    @Override
    public void run() {
        boolean acquired = quartzLock.tryLock();
        if (acquired) {
            if (quartz.isRunning()) {
                quartzLock.unlock();
            } else {
                LOGGER.info("acquire quartz lock successful which would start quartz schedule");
                quartz.start();
            }
        } else if (quartz.isRunning()) {
            LOGGER.info("acquire quartz lock failure which would stop quartz schedule");
            quartz.stop();
        }
    }
}
