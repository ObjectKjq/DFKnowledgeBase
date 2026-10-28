package com.df.knowledge.config;

import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

/** 配置 Spring Data 检索在超时或分片失败时拒绝返回部分结果。 */
@Configuration
public class ElasticsearchSearchConfiguration {

    /**
     * Spring Data SearchHits 不保留 timed_out，在 HTTP 请求层要求 ES 对不完整检索报错。
     *
     * @return 仅作用于检索请求的客户端配置
     */
    @Bean
    public Rest5ClientBuilderCustomizer completeSearchResultsCustomizer() {
        return new Rest5ClientBuilderCustomizer() {
            @Override
            public void customize(Rest5ClientBuilder builder) {
                // 使用 HTTP 专用回调追加配置，保留 starter 提供的认证和连接配置。
            }

            @Override
            public void customize(HttpAsyncClientBuilder client) {
                client.addRequestInterceptorLast((request, entity, context) -> {
                    try {
                        var uri = new URIBuilder(request.getUri());
                        if (uri.getPath().endsWith("/_search")) {
                            request.setUri(uri.setParameter("allow_partial_search_results", "false").build());
                        }
                    } catch (URISyntaxException ex) {
                        throw new ProtocolException("Elasticsearch 请求地址无效", ex);
                    }
                });
            }
        };
    }
}
