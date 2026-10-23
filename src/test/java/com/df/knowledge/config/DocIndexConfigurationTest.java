package com.df.knowledge.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.df.knowledge.job.FileMigrationRunner;
import com.df.knowledge.service.doc.DocIngestServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 ES starter 在完整应用上下文中创建客户端与 Spring Data 操作接口。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"knowledge.doc.storage-path=./data/docs", "knowledge.model.mock-enabled=false",
                "knowledge.rag.base-url="})
class DocIndexConfigurationTest {

    @MockitoBean
    private FileMigrationRunner fileMigrationRunner;

    @MockitoBean
    private com.df.knowledge.service.model.LegacyModelConfigurationMigration legacyModelConfigurationMigration;

    @MockitoBean
    private DocIngestServiceImpl docIngestService;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @LocalServerPort
    private int port;

    @Test
    void testElasticsearchStarter_createsClientAndOperations() {
        // 断言
        assertNotNull(elasticsearchClient);
        assertNotNull(elasticsearchOperations);
    }

    @Test
    void testApiDocs_isAvailable() throws Exception {
        // 调用并断言
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v3/api-docs"))
                .GET().build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"openapi\""));
        }
    }
}
