package com.df.knowledge.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.doc.DocChunkIndexServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 Spring Data Template 和本地 HTTP 替身，验证请求序列化及完整检索配置。 */
class ElasticsearchSearchConfigurationTest {

    private final List<URI> requests = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final DocChunkIndexServiceImpl service = new DocChunkIndexServiceImpl();
    private HttpServer server;
    private ElasticsearchTransport transport;
    private volatile boolean rejectSearch;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            URI uri = exchange.getRequestURI();
            requests.add(uri);
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response;
            int status = 200;
            if (uri.getPath().endsWith("/_search")) {
                if (rejectSearch) {
                    status = 503;
                    response = """
                            {"error":{"type":"search_phase_execution_exception","reason":"partial results forbidden"},"status":503}
                            """;
                } else {
                    response = """
                            {"took":1,"timed_out":false,"_shards":{"total":1,"successful":1,"skipped":0,"failed":0},
                             "hits":{"total":{"value":1,"relation":"eq"},"max_score":1,
                             "hits":[{"_index":"knowledge_doc_chunk","_id":"100","_score":1}]}}
                            """;
                }
            } else if (uri.getPath().endsWith("/_bulk")) {
                response = """
                        {"took":1,"errors":false,"items":[{"index":{"_index":"knowledge_doc_chunk","_id":"100","status":201}}]}
                        """;
            } else if (uri.getPath().endsWith("/_delete_by_query")) {
                response = """
                        {"took":1,"timed_out":false,"total":1,"deleted":1,"batches":1,"version_conflicts":0,
                         "noops":0,"retries":{"bulk":0,"search":0},"throttled_millis":0,
                         "requests_per_second":-1,"throttled_until_millis":0,"failures":[]}
                        """;
            } else {
                response = "{\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0}}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("X-Elastic-Product", "Elasticsearch");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var customizer = new ElasticsearchSearchConfiguration().completeSearchResultsCustomizer();
        var builder = Rest5Client.builder(new HttpHost("http", "127.0.0.1", server.getAddress().getPort()));
        builder.setHttpClientConfigCallback(customizer::customize);
        customizer.customize(builder);
        transport = new Rest5ClientTransport(builder.build(), new JacksonJsonpMapper());
        var operations = new ElasticsearchTemplate(new ElasticsearchClient(transport));
        ReflectionTestUtils.setField(service, "elasticsearchOperations", operations);
        ReflectionTestUtils.setField(service, "indexName", "knowledge_doc_chunk");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (transport != null) transport.close();
        if (server != null) server.stop(0);
    }

    @Test
    void testOperations_serializesIndexDeleteAndSearchThroughSpringData() {
        // 准备参数
        DocChunkDO chunk = new DocChunkDO().setDocId(5L).setChunkIndex(0).setText("正文");
        chunk.setId(100L);

        // 调用：实际执行 Spring Data Template 的映射和请求转换。
        service.writeBatch(2L, List.of(chunk), List.of(new float[512]));
        service.deleteByDocId(5L);
        var result = service.search("正文", new float[512], List.of(2L), List.of(5L), "HYBRID", 20);

        // 断言：不改变现有索引结构，检索拒绝部分结果，其他请求不携带检索专用参数。
        assertEquals(List.of(100L), result.stream().map(hit -> hit.chunkId()).toList());
        assertTrue(requests.getFirst().getQuery().contains("refresh=wait_for"));
        assertTrue(bodies.getFirst().contains("\"kb_id\":\"2\""));
        assertFalse(bodies.getFirst().contains("_class"));
        for (URI uri : requests) {
            boolean completeResults = uri.getQuery() != null
                    && uri.getQuery().contains("allow_partial_search_results=false");
            assertEquals(uri.getPath().endsWith("/_search"), completeResults);
        }
    }

    @Test
    void testSearch_serverRejectsIncompleteResultsAsBusinessFailure() {
        // 准备参数：ES 对不完整检索返回错误。
        rejectSearch = true;

        // 调用并断言：不能返回伪装完整的结果。
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.search("正文", new float[512], List.of(2L), List.of(5L), "FUZZY", 20));
        assertEquals(ErrorCodeConstants.DOC_SEARCH_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(requests.getFirst().getQuery().contains("allow_partial_search_results=false"));
    }
}
