package com.df.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 入库任务线程池配置
 *
 * ADR-0007：入库管线用 Spring 线程池 + MySQL 状态机，不引入 MQ；并发度由池参数控制（PRD §8.4）
 */
@Configuration
public class DocIngestExecutorConfiguration {

    /**
     * 入库任务线程池
     *
     * 待处理任务保存在 MySQL，线程池饱和时拒绝领取，下一次轮询继续
     *
     * @return 入库任务线程池
     */
    @Bean
    public ThreadPoolTaskExecutor docIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("doc-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
