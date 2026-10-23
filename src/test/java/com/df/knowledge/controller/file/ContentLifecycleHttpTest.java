package com.df.knowledge.controller.file;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import com.df.knowledge.controller.auth.AuthController;
import com.df.knowledge.controller.knowledge.KnowledgeBaseMemberController;
import com.df.knowledge.controller.user.UserController;
import com.df.knowledge.job.DocPurgeJob;
import com.df.knowledge.service.auth.AuthServiceImpl;
import com.df.knowledge.service.permission.PermissionServiceImpl;
import com.df.knowledge.service.permission.StpInterfaceImpl;
import com.df.knowledge.service.user.UserServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.exception.GlobalExceptionHandler;
import com.df.knowledge.config.*;
import com.df.knowledge.controller.doc.DocController;
import com.df.knowledge.controller.knowledge.KnowledgeBaseController;
import com.df.knowledge.service.doc.*;
import com.df.knowledge.service.file.FileServiceImpl;
import com.df.knowledge.service.knowledge.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 文件授权及回收站的 HTTP 完整旅程，真实 MySQL 与业务服务。 */
@EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
@SpringBootTest(classes = ContentLifecycleHttpTest.Application.class, properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/df_knowledge_base_p0_19_test?allowPublicKeyRetrieval=true&useSSL=false",
        "spring.datasource.username=root", "spring.datasource.password=root",
        "spring.autoconfigure.exclude=org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration,org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration"
})
class ContentLifecycleHttpTest {
    static {
        // 使用专用测试库，绝不清理部署者数据库。
        try (var connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/?allowPublicKeyRetrieval=true&useSSL=false", "root", "root");
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS df_knowledge_base_p0_19_test CHARACTER SET utf8mb4");
            statement.execute("USE df_knowledge_base_p0_19_test");
            String schema = Files.readString(Path.of("sql/schema.sql"));
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
            for (String sql : Files.readString(Path.of("sql/data.sql")).split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.df.knowledge.dal.mysql")
    @Import({FileController.class, DocController.class, KnowledgeBaseController.class,
            GlobalExceptionHandler.class, MyBatisPlusConfiguration.class, DefaultDBFieldHandler.class,
            SaTokenConfiguration.class, FileServiceImpl.class, DocServiceImpl.class,
            KnowledgeBaseServiceImpl.class, KnowledgeBasePermissionServiceImpl.class,
            DocIngestServiceImpl.class, DocParseServiceImpl.class, DocIndexServiceImpl.class,
            DocIngestExecutorConfiguration.class, SecurityConfiguration.class,
            UserController.class,
            AuthController.class,
            KnowledgeBaseMemberController.class,
            UserServiceImpl.class,
            AuthServiceImpl.class,
            PermissionServiceImpl.class,
            StpInterfaceImpl.class,
            KnowledgeBaseMemberServiceImpl.class, DocPurgeJob.class})
    static class Application {
        @Bean DocChunkIndexService docChunkIndexService() { return mock(DocChunkIndexService.class); }
        @Bean DocEmbeddingService docEmbeddingService() { return texts -> texts.stream().map(text -> vector()).toList(); }
    }

    private static float[] vector() {
        float[] vector = new float[512];
        vector[0] = 1;
        return vector;
    }

    @Resource WebApplicationContext context;
    MockMvc mvc;
    String owner;
    String stranger;
    String admin;
    String memberName;
    @Resource JdbcTemplate jdbc;
    @Resource DocPurgeJob purgeJob;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void prepare() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(new SaTokenContextFilterForJakartaServlet()).build();
        admin = StpUtil.getStpLogic().createLoginSession(1L);
        String name = "owner" + System.nanoTime();
        memberName = "member" + System.nanoTime();
        createUser(name);
        createUser(memberName);
        owner = login(name);
        stranger = login(memberName);
    }

    private void createUser(String name) throws Exception {
        postJson(admin, "/system/user/create", "{\"username\":\"" + name
                + "\",\"nickname\":\"旅程用户\",\"password\":\"journey123\",\"roleIds\":[2]}", 0);
    }

    private String login(String name) throws Exception {
        return postJson(null, "/system/auth/login", "{\"username\":\"" + name
                + "\",\"password\":\"journey123\"}", 0).path("data").path("token").asText();
    }

    private JsonNode postJson(String token, String path, String body, int code) throws Exception {
        var request = post(path).contentType("application/json").content(body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return json.readTree(mvc.perform(request).andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString());
    }

    private long base(String token, int type) throws Exception {
        return postJson(token, "/knowledge/base/create", "{\"name\":\"旅程知识库\",\"type\":" + type + "}", 0).path("data").asLong();
    }

    private long upload(String token, String content) throws Exception {
        return json.readTree(mvc.perform(multipart("/system/file/upload")
                        .file(new MockMultipartFile("file", "evidence.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long document(String token, long base, long file, int code) throws Exception {
        return postJson(token, "/knowledge/doc/create", "{\"knowledgeBaseId\":" + base + ",\"fileId\":" + file + ",\"fileUrl\":\"/api/system/file/download?id=" + file + "\"}", code).path("data").asLong();
    }

    private JsonNode getJson(String token, String path) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
    }

    private void action(String token, String path, boolean deletion, int code) throws Exception {
        mvc.perform((deletion ? delete(path) : post(path)).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(code));
    }

    private void awaitStatus(String token, long id, int status) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (getJson(token, "/knowledge/doc/get?id=" + id).path("data").path("status").asInt() == status) return;
            Thread.sleep(50);
        }
        fail("文档未到达预期状态：" + getJson(token, "/knowledge/doc/get?id=" + id));
    }

    @Test
    void testFile_unassociatedUploadIsPrivate() throws Exception {
        // 准备参数：用户上传未关联文件。
        String result = mvc.perform(multipart("/system/file/upload")
                        .file(new MockMultipartFile("file", "private.txt", "text/plain", "private evidence".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(result).path("data").path("id").asLong();
        // 调用与断言：陌生人不能读取、删除，也不能借关联获得权限。
        mvc.perform(get("/system/file/download").param("id", "" + id).header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.code").value(1001005001));
        mvc.perform(delete("/system/file/delete").param("id", "" + id).header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.code").value(1001005001));
        mvc.perform(get("/system/file/download").param("id", "" + id).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("private evidence"));
    }
    @Test
    void testMembership_downloadAndAssociationFollowCurrentRole() throws Exception {
        // 准备参数：共享知识库的查看者可以下载，但不能写入或复制内容。
        long shared = base(owner, 2);
        long personal = base(stranger, 1);
        long file = upload(owner, "shared evidence");
        long doc = document(owner, shared, file, 0);
        postJson(owner, "/knowledge/base/member/invite", "{\"knowledgeBaseId\":" + shared
                + ",\"username\":\"" + memberName + "\",\"memberRole\":2}", 0);
        mvc.perform(get("/system/file/download?id=" + file).header("Authorization", "Bearer " + stranger))
                .andExpect(content().string("shared evidence"));
        document(stranger, personal, file, 1001005001);
        action(stranger, "/knowledge/doc/delete?id=" + doc, true, 1001002003);
        // 调用：所有者升级为编辑者，编辑者上传后再被移除。
        long member = getJson(owner, "/knowledge/base/member/list?knowledgeBaseId=" + shared).path("data").get(0).path("id").asLong();
        mvc.perform(put("/knowledge/base/member/update-role").contentType("application/json")
                .content("{\"knowledgeBaseId\":" + shared + ",\"id\":" + member + ",\"memberRole\":1}")
                .header("Authorization", "Bearer " + owner)).andExpect(jsonPath("$.code").value(0));
        long memberFile = upload(stranger, "member evidence");
        document(stranger, shared, memberFile, 0);
        action(owner, "/knowledge/base/member/delete?knowledgeBaseId=" + shared + "&id=" + member, true, 0);
        // 断言：原上传者和实例管理员也不拥有越权例外。
        assertEquals(1001005001, getJson(stranger, "/system/file/download?id=" + memberFile).path("code").asInt());
        assertEquals(1001005001, getJson(admin, "/system/file/download?id=" + memberFile).path("code").asInt());
        assertEquals(0, getJson(stranger, "/knowledge/base/page").path("code").asInt());
    }

    @Test
    void testKnowledgeBase_recycleRestorePreservesMembersAndPurgeNeedsConfirmation() throws Exception {
        // 准备参数：共享知识库与成员、可用文档。
        long shared = base(owner, 2);
        postJson(owner, "/knowledge/base/member/invite", "{\"knowledgeBaseId\":" + shared
                + ",\"username\":\"" + memberName + "\",\"memberRole\":2}", 0);
        long file = upload(owner, "restore evidence");
        long doc = document(owner, shared, file, 0);
        awaitStatus(owner, doc, 4);
        // 调用与断言：软删除即时隐藏，只有原所有者能看到回收站与恢复。
        action(owner, "/knowledge/base/delete?id=" + shared, true, 0);
        assertEquals(1001002001, getJson(stranger, "/knowledge/base/get?id=" + shared).path("code").asInt());
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
        assertEquals(0, getJson(stranger, "/knowledge/base/recycle-list").path("data").size());
        action(stranger, "/knowledge/base/restore?id=" + shared, false, 1001002001);
        action(owner, "/knowledge/base/restore?id=" + shared, false, 0);
        awaitStatus(owner, doc, 4);
        assertEquals(1, getJson(owner, "/knowledge/base/member/list?knowledgeBaseId=" + shared).path("data").size());
        mvc.perform(get("/system/file/download?id=" + file).header("Authorization", "Bearer " + stranger))
                .andExpect(content().string("restore evidence"));
        action(owner, "/knowledge/base/delete?id=" + shared, true, 0);
        action(owner, "/knowledge/base/purge?id=" + shared, true, 1001002009);
        action(owner, "/knowledge/base/purge?id=" + shared + "&confirmed=true", true, 0);
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
    }

    @Test
    void testDocuments_sharedBytesSurviveUntilLastRetainedReference() throws Exception {
        // 准备参数：同一文件被两个业务文档引用。
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "retained evidence");
        long first = document(owner, knowledgeBase, file, 0);
        long second = document(owner, knowledgeBase, file, 0);
        awaitStatus(owner, first, 4);
        awaitStatus(owner, second, 4);
        // 调用与断言：清理首个引用不会破坏第二个，回收站引用也保留字节。
        action(owner, "/knowledge/doc/delete?id=" + first, true, 0);
        action(owner, "/knowledge/doc/purge?id=" + first, true, 1001003015);
        action(owner, "/knowledge/doc/purge?id=" + first + "&confirmed=true", true, 0);
        mvc.perform(get("/system/file/download?id=" + file).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("retained evidence"));
        action(owner, "/knowledge/doc/delete?id=" + second, true, 0);
        action(owner, "/knowledge/doc/restore?id=" + second, false, 0);
        awaitStatus(owner, second, 4);
        mvc.perform(get("/system/file/download?id=" + file).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("retained evidence"));
        action(owner, "/knowledge/doc/delete?id=" + second, true, 0);
        action(owner, "/knowledge/doc/purge?id=" + second + "&confirmed=true", true, 0);
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
    }

    @Test
    void testRetention_expiredDocumentsAndKnowledgeBasesCannotRestore() throws Exception {
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "expired evidence");
        long doc = document(owner, knowledgeBase, file, 0);
        awaitStatus(owner, doc, 4);
        action(owner, "/knowledge/doc/delete?id=" + doc, true, 0);
        // 时间边界是外部输入；只以 HTTP 验证结果，不断言数据库实现。
        jdbc.update("UPDATE knowledge_doc SET delete_time = DATE_SUB(NOW(), INTERVAL 30 DAY) WHERE id = ?", doc);
        action(owner, "/knowledge/doc/restore?id=" + doc, false, 1001003007);
        purgeJob.execute();
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
        action(owner, "/knowledge/base/delete?id=" + knowledgeBase, true, 0);
        jdbc.update("UPDATE knowledge_base SET delete_time = DATE_SUB(NOW(), INTERVAL 30 DAY) WHERE id = ?", knowledgeBase);
        action(owner, "/knowledge/base/restore?id=" + knowledgeBase, false, 1001002008);
        purgeJob.execute();
        assertEquals(0, getJson(owner, "/knowledge/base/recycle-list").path("data").size());
    }

    @Test
    void testIdentity_defaultBaseAndLogout() throws Exception {
        // 准备参数、调用与断言：默认库不可删除，普通用户无用户管理权限，登出立即失效。
        var bases = getJson(owner, "/knowledge/base/page").path("data").path("list");
        long defaultId = bases.get(0).path("id").asLong();
        assertEquals("我的知识库", bases.get(0).path("name").asText());
        action(owner, "/knowledge/base/delete?id=" + defaultId, true, 1001002002);
        postJson(owner, "/system/user/create", "{\"username\":\"forbidden\",\"nickname\":\"普通用户\",\"password\":\"journey123\"}", 403);
        action(owner, "/system/auth/logout", false, 0);
        assertEquals(401, getJson(owner, "/knowledge/base/page").path("code").asInt());
    }

    @Test
    void testDocuments_unavailableAndUnauthorizedAreIndistinguishable() throws Exception {
        // 准备参数：个人知识库内容对非成员不可探测。
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "private document");
        long doc = document(owner, knowledgeBase, file, 0);
        assertEquals(getJson(stranger, "/knowledge/doc/get?id=9223372036854775807").path("code"),
                getJson(stranger, "/knowledge/doc/get?id=" + doc).path("code"));
    }

    @Test
    void testRestore_indexFailureDoesNotPublishAvailableAndFailedStateSurvives() throws Exception {
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "reindex evidence");
        long doc = document(owner, knowledgeBase, file, 0);
        awaitStatus(owner, doc, 4);
        action(owner, "/knowledge/doc/delete?id=" + doc, true, 0);
        // 外部 ES 在恢复时不可用，不能假称文档已可检索。
        var index = context.getBean(DocChunkIndexService.class);
        Mockito.doThrow(new IllegalStateException("外部索引不可用"))
                .when(index).writeBatch(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
        try {
            action(owner, "/knowledge/doc/restore?id=" + doc, false, 0);
            awaitStatus(owner, doc, 5);
            String failure = getJson(owner, "/knowledge/doc/get?id=" + doc).path("data").path("failReason").asText();
            action(owner, "/knowledge/doc/delete?id=" + doc, true, 0);
            action(owner, "/knowledge/doc/restore?id=" + doc, false, 0);
            assertEquals(5, getJson(owner, "/knowledge/doc/get?id=" + doc).path("data").path("status").asInt());
            assertEquals(failure, getJson(owner, "/knowledge/doc/get?id=" + doc).path("data").path("failReason").asText());
        } finally {
            Mockito.reset(index);
        }
        action(owner, "/knowledge/doc/retry?id=" + doc, false, 0);
        awaitStatus(owner, doc, 4);
    }

    @Test
    void testMigration_legacyUploadOwnershipAndKnowledgeBaseRecycleRemainUsable() throws Exception {
        // 准备参数：模拟 011 的可信 creator、旧逻辑删除知识库及保留成员。
        long knowledgeBase = base(owner, 2);
        postJson(owner, "/knowledge/base/member/invite", "{\"knowledgeBaseId\":" + knowledgeBase
                + ",\"username\":\"" + memberName + "\",\"memberRole\":2}", 0);
        long privateFile = upload(owner, "legacy private upload");
        long unknownFile = upload(owner, "legacy unknown owner");
        jdbc.update("UPDATE system_file SET creator = '' WHERE id = ?", unknownFile);
        jdbc.update("UPDATE knowledge_base SET deleted = b'1' WHERE id = ?", knowledgeBase);
        jdbc.execute("ALTER TABLE system_file DROP COLUMN uploader_id");
        jdbc.execute("ALTER TABLE knowledge_base DROP COLUMN delete_time");
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new FileSystemResource("sql/upgrade/012-content-lifecycle.sql"));
        }
        // 断言：迁移不会公开无归属文件，原所有者可恢复旧库及仍然存在的成员关系。
        mvc.perform(get("/system/file/download?id=" + privateFile).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("legacy private upload"));
        assertEquals(1001005001, getJson(stranger, "/system/file/download?id=" + privateFile).path("code").asInt());
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + unknownFile).path("code").asInt());
        action(owner, "/knowledge/base/restore?id=" + knowledgeBase, false, 0);
        assertEquals(1, getJson(owner, "/knowledge/base/member/list?knowledgeBaseId=" + knowledgeBase).path("data").size());
    }

    @Test
    void testRestore_keepsDocumentIndexingUntilExternalIndexCompletes() throws Exception {
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "pending reindex evidence");
        long doc = document(owner, knowledgeBase, file, 0);
        awaitStatus(owner, doc, 4);
        action(owner, "/knowledge/doc/delete?id=" + doc, true, 0);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var index = context.getBean(DocChunkIndexService.class);
        Mockito.doAnswer(invocation -> {
            entered.countDown();
            if (!release.await(30, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("索引替身等待超时");
            return null;
        }).when(index).writeBatch(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
        try {
            action(owner, "/knowledge/doc/restore?id=" + doc, false, 0);
            assertTrue(entered.await(30, java.util.concurrent.TimeUnit.SECONDS));
            // 只有外部索引真正完成后，HTTP 才能呈现「可用」。
            assertEquals(3, getJson(owner, "/knowledge/doc/get?id=" + doc).path("data").path("status").asInt());
        } finally {
            release.countDown();
        }
        awaitStatus(owner, doc, 4);
        Mockito.reset(index);
    }

    @Test
    void testKnowledgeBase_restoreRetainsFailureAndRequeuesInterruptedDocument() throws Exception {
        long knowledgeBase = base(owner, 2);
        long failed = document(owner, knowledgeBase, upload(owner, "   "), 0);
        long interrupted = document(owner, knowledgeBase, upload(owner, "interrupted evidence"), 0);
        awaitStatus(owner, failed, 5);
        awaitStatus(owner, interrupted, 4);
        String failure = getJson(owner, "/knowledge/doc/get?id=" + failed).path("data").path("failReason").asText();
        // 模拟上次进程在解析阶段停止的持久状态，恢复必须重新处理。
        jdbc.update("UPDATE knowledge_doc SET status = 2 WHERE id = ?", interrupted);
        action(owner, "/knowledge/base/delete?id=" + knowledgeBase, true, 0);
        action(owner, "/knowledge/base/restore?id=" + knowledgeBase, false, 0);
        assertEquals(5, getJson(owner, "/knowledge/doc/get?id=" + failed).path("data").path("status").asInt());
        assertEquals(failure, getJson(owner, "/knowledge/doc/get?id=" + failed).path("data").path("failReason").asText());
        awaitStatus(owner, interrupted, 4);
    }

}


