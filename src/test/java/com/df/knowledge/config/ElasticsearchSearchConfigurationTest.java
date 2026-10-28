package com.df.knowledge.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocChunkDocument;
import com.df.knowledge.dal.elasticsearch.doc.DocChunkRepository;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.doc.DocChunkIndexServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactory;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 Spring Data Template 和本地 HTTP 替身，验证请求序列化及完整检索配置。 */
class ElasticsearchSearchConfigurationTest {

    private final List<URI> requests = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final DocChunkIndexServiceImpl service = new DocChunkIndexServiceImpl();
    private HttpServer server;
    private ElasticsearchTransport transport;
    private GenericApplicationContext applicationContext;
    private ElasticsearchTemplate operations;
    private volatile boolean rejectSearch;
    private volatile boolean rejectBulk;

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
                response = rejectBulk ? """
                        {"took":1,"errors":true,"items":[
                         {"index":{"_index":"knowledge_doc_chunk_override","_id":"100","status":201}},
                         {"index":{"_index":"knowledge_doc_chunk_override","_id":"101","status":400,
                          "error":{"type":"mapper_parsing_exception","reason":"向量维度错误"}}}]}
                        """ : """
                        {"took":1,"errors":false,"items":[{"index":{"_index":"knowledge_doc_chunk_override","_id":"100","status":201}}]}
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
        applicationContext = new GenericApplicationContext();
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                "knowledge.elasticsearch.index-name=knowledge_doc_chunk_override");
        applicationContext.refresh();
        var mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setApplicationContext(applicationContext);
        var converter = new MappingElasticsearchConverter(mappingContext);
        converter.setApplicationContext(applicationContext);
        converter.afterPropertiesSet();
        operations = new ElasticsearchTemplate(new ElasticsearchClient(transport), converter);
        var repository = new ElasticsearchRepositoryFactory(operations).getRepository(DocChunkRepository.class);
        ReflectionTestUtils.setField(service, "elasticsearchOperations", operations);
        ReflectionTestUtils.setField(service, "docChunkRepository", repository);
        // Repository 初始化不得向 ES 创建索引或更新 mapping。
        assertTrue(requests.isEmpty());
    }

    @Test
    void testRepository_partialBulkFailureRemainsBusinessFailure() {
        // 准备参数：批量请求整体成功，但第二个分块写入失败。
        rejectBulk = true;
        DocChunkDO first = new DocChunkDO().setDocId(5L).setChunkIndex(0).setText("第一块");
        first.setId(100L);
        DocChunkDO second = new DocChunkDO().setDocId(5L).setChunkIndex(1).setText("第二块");
        second.setId(101L);

        // 调用并断言：经过真实 Repository 和 Template 后仍拒绝部分成功。
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.writeBatch(2L, List.of(first, second), List.of(new float[512], new float[512])));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("向量维度错误"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (transport != null) transport.close();
        if (server != null) server.stop(0);
        if (applicationContext != null) applicationContext.close();
    }

    @Test
    void testOperations_serializesIndexDeleteAndSearchThroughSpringData() {
        // 准备参数
        DocChunkDO chunk = new DocChunkDO().setDocId(5L).setChunkIndex(0).setText("正文")
                .setStartOffset(0).setEndOffset(2).setPageNumber(1);
        chunk.setId(100L);

        // 调用：实际执行 Spring Data Template 的映射和请求转换。
        service.writeBatch(2L, List.of(chunk), List.of(new float[512]));
        service.deleteByDocId(5L);
        var result = service.search("正文", new float[512], List.of(2L), List.of(5L), "HYBRID", 20);

        // 断言：不改变现有索引结构，检索拒绝部分结果，其他请求不携带检索专用参数。
        assertEquals(List.of(100L), result.stream().map(hit -> hit.chunkId()).toList());
        assertTrue(requests.getFirst().getQuery().contains("refresh=wait_for"));
        List<String> bulkLines = bodies.getFirst().lines().toList();
        Map<?, ?> metadata = (Map<?, ?>) Document.parse(bulkLines.getFirst()).get("index");
        assertEquals("100", metadata.get("_id"));
        assertEquals("knowledge_doc_chunk_override", metadata.get("_index"));
        Document source = Document.parse(bulkLines.get(1));
        assertEquals(Set.of("kb_id", "doc_id", "chunk_id", "chunk_index", "text", "start_offset",
                "end_offset", "page_number", "vector"), source.keySet());
        assertEquals("2", source.get("kb_id"));
        assertEquals("5", source.get("doc_id"));
        assertEquals("100", source.get("chunk_id"));
        assertEquals(0, source.get("chunk_index"));
        assertEquals("正文", source.get("text"));
        assertEquals(0, source.get("start_offset"));
        assertEquals(2, source.get("end_offset"));
        assertEquals(1, source.get("page_number"));
        assertEquals(512, ((List<?>) source.get("vector")).size());
        for (URI uri : requests) {
            // 批量操作的索引位于请求体，其余请求均从实体解析同一个配置索引。
            if (!uri.getPath().endsWith("/_bulk")) {
                assertTrue(uri.getPath().startsWith("/knowledge_doc_chunk_override/"));
            }
            boolean completeResults = uri.getQuery() != null
                    && uri.getQuery().contains("allow_partial_search_results=false");
            assertEquals(uri.getPath().endsWith("/_search"), completeResults);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDocument_mappingMatchesDeploymentScript() throws Exception {
        // 准备参数：部署脚本是已有索引契约，实体注解必须与它一致。
        String script = Files.readString(Path.of("es/create-doc-chunk-index.http"));
        Map<String, Object> expected = (Map<String, Object>) Document.parse(script.substring(script.indexOf("\n{") + 1))
                .get("mappings");

        // 调用：在本地生成注解映射，不向 ES 写入。
        Document actual = operations.indexOps(DocChunkDocument.class).createMapping();

        // 断言：字段、类型、分词器和向量参数兼容严格 mapping。
        assertEquals(expected.get("dynamic"), actual.get("dynamic"));
        Map<String, Map<String, Object>> expectedFields = (Map<String, Map<String, Object>>) expected.get("properties");
        Map<String, Map<String, Object>> actualFields = (Map<String, Map<String, Object>>) actual.get("properties");
        assertEquals(expectedFields.keySet(), actualFields.keySet());
        expectedFields.forEach((name, field) -> field.forEach((attribute, value) -> {
            if (value instanceof Map<?, ?> options) {
                Map<?, ?> actualOptions = (Map<?, ?>) actualFields.get(name).get(attribute);
                options.forEach((key, option) -> assertEquals(option, actualOptions.get(key), name + "." + key));
            } else if ("index".equals(attribute)) {
                // Spring Data 省略默认值 true，与 ES 默认启用向量索引的行为等价。
                assertEquals(value, actualFields.get(name).getOrDefault(attribute, true), name + "." + attribute);
            } else {
                assertEquals(value, actualFields.get(name).get(attribute), name + "." + attribute);
            }
        }));
        assertTrue(requests.isEmpty());
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
