package com.df.knowledge.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 文档索引的 ES 客户端与内置中文向量模型配置
 */
@Configuration
public class DocIndexConfiguration {

    /**
     * 创建 ES 客户端，不在应用内创建或修改索引（ADR-0006）
     *
     * @param url ES 服务地址
     * @param objectMapper 公共 JSON 序列化器
     * @return ES 客户端，应用关闭时释放连接
     */
    @Bean(destroyMethod = "close")
    public ElasticsearchClient elasticsearchClient(@Value("${knowledge.elasticsearch.url}") String url,
                                                 ObjectMapper objectMapper) {
        return ElasticsearchClient.of(builder -> builder.host(url).jsonMapper(new JacksonJsonpMapper(objectMapper)));
    }

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
