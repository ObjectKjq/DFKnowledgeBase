package com.df.knowledge.controller.file;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import com.df.knowledge.controller.auth.AuthController;
import com.df.knowledge.controller.knowledge.KnowledgeBaseMemberController;
import com.df.knowledge.controller.user.UserController;
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

/** 文件授权及删除的 HTTP 完整旅程，真实 MySQL 与业务服务。 */
@EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
@SpringBootTest(classes = ContentLifecycleHttpTest.Application.class, properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/df_knowledge_base_p0_19_test?allowPublicKeyRetrieval=true&useSSL=false",
        "spring.datasource.username=${P0_MYSQL_USER:root}", "spring.datasource.password=${P0_MYSQL_PASSWORD:root}",
        "spring.autoconfigure.exclude=org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration,org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration"
})
class ContentLifecycleHttpTest {
    static {
        // 使用专用测试库，绝不清理部署者数据库。
        try (var connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/?allowPublicKeyRetrieval=true&useSSL=false",
                System.getenv().getOrDefault("P0_MYSQL_USER", "root"),
                System.getenv().getOrDefault("P0_MYSQL_PASSWORD", "root"));
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
    @org.springframework.scheduling.annotation.EnableScheduling
    @MapperScan("com.df.knowledge.dal.mysql")
    @Import({FileController.class, DocController.class, KnowledgeBaseController.class,
            GlobalExceptionHandler.class, MyBatisPlusConfiguration.class, DefaultDBFieldHandler.class,
            SaTokenConfiguration.class, FileServiceImpl.class, DocServiceImpl.class,
            KnowledgeBaseServiceImpl.class, KnowledgeBasePermissionServiceImpl.class,
            DocIngestServiceImpl.class, DocParseServiceImpl.class, DocIndexServiceImpl.class,
            com.df.knowledge.service.doc.DocOcrServiceImpl.class,
            com.df.knowledge.service.model.InstanceSettingsServiceImpl.class,
            DocIngestExecutorConfiguration.class, SecurityConfiguration.class,
            UserController.class,
            AuthController.class,
            KnowledgeBaseMemberController.class,
            UserServiceImpl.class,
            AuthServiceImpl.class,
            PermissionServiceImpl.class,
            StpInterfaceImpl.class,
            KnowledgeBaseMemberServiceImpl.class})
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
    void testDocuments_sharedBytesSurviveUntilLastReference() throws Exception {
        // 准备参数：同一文件被两个业务文档引用。
        long knowledgeBase = base(owner, 1);
        long file = upload(owner, "retained evidence");
        long first = document(owner, knowledgeBase, file, 0);
        long second = document(owner, knowledgeBase, file, 0);
        context.getBean(DocIngestService.class).processDoc(first);
        context.getBean(DocIngestService.class).processDoc(second);
        // 调用与断言：删除首个引用不会破坏第二个，最后一个引用移除后回收字节。
        action(owner, "/knowledge/doc/delete?id=" + first, true, 0);
        mvc.perform(get("/system/file/download?id=" + file).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("retained evidence"));
        action(owner, "/knowledge/doc/delete?id=" + second, true, 0);
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
    }

    @Test
    void testKnowledgeBase_deleteRemovesDocumentsMembersAndFiles() throws Exception {
        // 准备参数：具有成员和待解析文档的共享知识库。
        long shared = base(owner, 2);
        postJson(owner, "/knowledge/base/member/invite", "{\"knowledgeBaseId\":" + shared
                + ",\"username\":\"" + memberName + "\",\"memberRole\":2}", 0);
        long file = upload(owner, "deleted knowledge base evidence");
        long doc = document(owner, shared, file, 0);

        // 调用
        action(owner, "/knowledge/base/delete?id=" + shared, true, 0);

        // 断言：删除后业务内容不可见，文档和成员关系均已清理。
        assertEquals(1001002001, getJson(owner, "/knowledge/base/get?id=" + shared).path("code").asInt());
        assertEquals(1001003001, getJson(owner, "/knowledge/doc/get?id=" + doc).path("code").asInt());
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + file).path("code").asInt());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_base_member WHERE knowledge_base_id = ?", Integer.class, shared));
    }

    @Test
    void testMigration_removesLegacyFieldsAndKeepsSharedFiles() throws Exception {
        // 准备参数：模拟旧版回收站文档、已删除知识库和仍然有效的共享引用。
        long retainedBase = base(owner, 1);
        long removedBase = base(owner, 1);
        long sharedFile = upload(owner, "shared migration evidence");
        long removedFile = upload(owner, "removed migration evidence");
        long removedDoc = document(owner, retainedBase, sharedFile, 0);
        long retainedDoc = document(owner, retainedBase, sharedFile, 0);
        long removedBaseDoc = document(owner, removedBase, removedFile, 0);
        jdbc.execute("ALTER TABLE knowledge_base ADD COLUMN delete_time datetime NULL");
        jdbc.execute("ALTER TABLE knowledge_doc ADD COLUMN prev_status tinyint NULL, ADD COLUMN delete_time datetime NULL");
        jdbc.update("UPDATE knowledge_doc SET status = 6, delete_time = NOW() WHERE id = ?", removedDoc);
        jdbc.update("UPDATE knowledge_base SET delete_time = NOW() WHERE id = ?", removedBase);

        // 调用：仅对隔离测试库执行历史数据清理及字段移除。
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/upgrade/017-remove-recycle-bin.sql"));
        }

        // 断言：有效共享引用保留，旧删除对象及无引用文件不再存在。
        assertEquals(0, getJson(owner, "/knowledge/doc/get?id=" + retainedDoc).path("code").asInt());
        assertEquals(1001003001, getJson(owner, "/knowledge/doc/get?id=" + removedDoc).path("code").asInt());
        assertEquals(1001003001, getJson(owner, "/knowledge/doc/get?id=" + removedBaseDoc).path("code").asInt());
        mvc.perform(get("/system/file/download?id=" + sharedFile).header("Authorization", "Bearer " + owner))
                .andExpect(content().string("shared migration evidence"));
        assertEquals(1001005001, getJson(owner, "/system/file/download?id=" + removedFile).path("code").asInt());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name IN ('knowledge_base', 'knowledge_doc') AND column_name IN ('delete_time', 'prev_status')", Integer.class));
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

}
