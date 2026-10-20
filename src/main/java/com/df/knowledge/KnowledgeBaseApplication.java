package com.df.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 本地化知识库平台 · 启动类
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务（文档超期彻底删除 DocPurgeJob）
public class KnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
}
