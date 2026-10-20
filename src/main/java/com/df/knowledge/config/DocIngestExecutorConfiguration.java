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
     * 拒绝策略用 CallerRunsPolicy：队列满时由提交线程执行，任务不丢弃
     *
     * @return 入库任务线程池
     */
    @Bean
    public ThreadPoolTaskExecutor docIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
