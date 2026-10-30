package com.df.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 问答流式任务执行器配置
 */
@Configuration
public class RagStreamExecutorConfiguration {

    /**
     * 使用虚拟线程承载等待模型响应的问答任务，应用退出时关闭
     *
     * @return 问答任务执行器
     */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService ragStreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 将阻塞的会话发送交给虚拟线程，取消订阅时中断任务。
     *
     * @param ragStreamExecutor 问答任务执行器
     * @return 会话流式任务调度器
     */
    @Bean(destroyMethod = "dispose")
    public Scheduler ragStreamScheduler(ExecutorService ragStreamExecutor) {
        return Schedulers.fromExecutorService(ragStreamExecutor);
    }

    /**
     * 主动结束超时问答，保证在 Servlet 关闭响应前发送错误事件
     *
     * @return 问答超时调度器
     */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ragStreamTimeoutExecutor() {
        return Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("rag-timeout").factory());
    }
}
