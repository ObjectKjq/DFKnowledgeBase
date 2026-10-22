package com.df.knowledge.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 文档索引的内置中文向量模型配置
 */
@Configuration
public class DocIndexConfiguration {

    /**
     * 模型随依赖打包，在首次入库时加载，无需配置外部推理服务（ADR-0008）
     *
     * @return 固定 512 维的中文向量模型
     */
    @Bean
    @Lazy
    public EmbeddingModel docEmbeddingModel() {
        // 并发由入库线程池控制；不复用同一池再提交子任务，避免工作线程互相等待
        return new BgeSmallZhV15EmbeddingModel(Runnable::run);
    }
}
