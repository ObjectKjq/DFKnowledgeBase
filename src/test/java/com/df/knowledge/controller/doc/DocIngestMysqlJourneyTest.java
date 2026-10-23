package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.doc.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.SqlSessionTemplate;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.df.knowledge.config.DefaultDBFieldHandler;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.df.knowledge.controller.doc.DocIngestJourneyTest.wire;

/** 显式开启的真实 MySQL 持久入库旅程；只重建名称固定的隔离测试库。 */
@EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
class DocIngestMysqlJourneyTest {
    private DriverManagerDataSource dataSource(String database) {
        return new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                System.getenv().getOrDefault("P0_MYSQL_USER", "root"),
                System.getenv().getOrDefault("P0_MYSQL_PASSWORD", "root"));
    }

    private SqlSessionTemplate newSession() throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource("df_knowledge_base_p0_21_test"));
        factory.setConfiguration(configuration);
        var global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig());
        global.setMetaObjectHandler(new DefaultDBFieldHandler());
        factory.setGlobalConfig(global);
        var sessions = factory.getObject();
        sessions.getConfiguration().addMapper(DocMapper.class);
        sessions.getConfiguration().addMapper(DocChunkMapper.class);
        return new SqlSessionTemplate(sessions);
    }

    @Test
    void testCreate_taskSurvivesRecreatedSchedulerAndMapperSession() throws Exception {
        // 准备参数：只允许固定隔离数据库，绝不重建业务库。
        new JdbcTemplate(dataSource("mysql")).execute("CREATE DATABASE IF NOT EXISTS df_knowledge_base_p0_21_test CHARACTER SET utf8mb4");
        String legacySchema = java.nio.file.Files.readString(java.nio.file.Path.of("sql/schema.sql")).lines()
                .filter(line -> !line.contains("`ingest_submit_time`") && !line.contains("`sheet_name`")
                        && !line.contains("`ocr`") && !line.contains("idx_ingest_schedule"))
                .collect(java.util.stream.Collectors.joining("\n"));
        new ResourceDatabasePopulator(new org.springframework.core.io.ByteArrayResource(legacySchema.getBytes(StandardCharsets.UTF_8)))
                .execute(dataSource("df_knowledge_base_p0_21_test"));
        var legacy = new JdbcTemplate(dataSource("df_knowledge_base_p0_21_test"));
        legacy.update("INSERT INTO knowledge_base (id,name,type,owner_id) VALUES (1,'资料',1,7)");
        legacy.update("INSERT INTO knowledge_doc (id, knowledge_base_id, title, file_name, format, file_size, file_id, status) VALUES (1,1,?,?,?,?,10,?)",
                "存量.txt", "存量.txt", com.df.knowledge.enums.doc.DocFormatEnum.TXT.getCode(), 12,
                DocStatusEnum.AVAILABLE.getCode());
        legacy.update("INSERT INTO knowledge_doc_chunk (id,doc_id,chunk_index,text,start_offset,end_offset) VALUES (1,1,0,'存量内容',0,4)");
        new ResourceDatabasePopulator(new FileSystemResource("sql/upgrade/014-ingest-position.sql"))
                .execute(dataSource("df_knowledge_base_p0_21_test"));
        var first = newSession();
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new DocIngestJourneyTest.Journey("持久任务.txt", "真实数据库中等待重启的内容".getBytes(StandardCharsets.UTF_8),
                    first.getMapper(DocMapper.class), first.getMapper(DocChunkMapper.class));
            var result = journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk()).andReturn();
            Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data");
            journey.mvc.perform(get("/knowledge/doc/get").param("id", id.toString()))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.QUEUED.getCode()));
            // 调用：以全新 mapper 会话和调度器恢复，不共享先前的内存任务表。
            var next = newSession();
            var restarted = new DocIngestJourneyTest.Journey("持久任务.txt", "真实数据库中等待重启的内容".getBytes(StandardCharsets.UTF_8),
                    next.getMapper(DocMapper.class), next.getMapper(DocChunkMapper.class));
            var executor = new com.df.knowledge.config.DocIngestExecutorConfiguration().docIngestExecutor();
            executor.initialize(); wire(restarted.ingest, "docIngestExecutor", executor);
            try {
                restarted.ingest.recoverInterruptedDocs();
            } finally {
                executor.getThreadPoolExecutor().shutdown();
                org.junit.jupiter.api.Assertions.assertTrue(executor.getThreadPoolExecutor().awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS));
            }
            // 断言：从 HTTP 观察持久状态及实际分块，未通过数据表侧路断言。
            restarted.mvc.perform(get("/knowledge/doc/get").param("id", id.toString()))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.AVAILABLE.getCode()));
            restarted.mvc.perform(get("/knowledge/search/preview").param("chunkId", "2"))
                    .andExpect(jsonPath("$.data.text").value("真实数据库中等待重启的内容"));
            restarted.mvc.perform(get("/knowledge/search/preview").param("chunkId", "1"))
                    .andExpect(jsonPath("$.data.text").value("存量内容"))
                    .andExpect(jsonPath("$.data.ocr").value(false));
        }
    }
}
