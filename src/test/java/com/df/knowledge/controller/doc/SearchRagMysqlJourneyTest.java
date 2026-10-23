package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.exception.GlobalExceptionHandler;
import com.df.knowledge.config.SaTokenConfiguration;
import com.df.knowledge.controller.conversation.ConversationController;
import com.df.knowledge.controller.rag.RagController;
import com.df.knowledge.service.doc.*;
import com.df.knowledge.service.conversation.*;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionServiceImpl;
import com.df.knowledge.service.model.*;
import com.df.knowledge.service.permission.*;
import com.df.knowledge.service.rag.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.*;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 真实 MySQL 与 HTTP 的会话、选定上下文、撤权及引用旅程，只有外部模型和索引使用可控替身。 */
@SpringBootTest(classes=SearchRagMysqlJourneyTest.App.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"spring.profiles.active=test","knowledge.model.mock-enabled=true","server.servlet.context-path=","spring.http.converters.preferred-json-mapper=jackson2"})
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="p0.mysql.enabled",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchRagMysqlJourneyTest {
 static final String USER=System.getenv().getOrDefault("P0_MYSQL_USER","root");
 static final String PASSWORD=System.getenv().getOrDefault("P0_MYSQL_PASSWORD","root");
 static final String DB="df_p0_search_"+ProcessHandle.current().pid();
 @LocalServerPort int port;
 final ObjectMapper json=new ObjectMapper();
 @org.springframework.beans.factory.annotation.Autowired RagGenerationPort generation;
 @org.springframework.beans.factory.annotation.Autowired DocChunkIndexService index;
 String token;
 @DynamicPropertySource static void database(DynamicPropertyRegistry r) throws Exception {
  try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/",USER,PASSWORD)){c.createStatement().execute("CREATE DATABASE "+DB);}
  try(var c=connection()){
   ScriptUtils.executeSqlScript(c,new FileSystemResource("sql/schema.sql"));
   c.createStatement().execute("INSERT INTO knowledge_base(id,name,type,owner_id) VALUES(1,'共享知识',2,7),(2,'私密知识',1,8)");
   c.createStatement().execute("INSERT INTO knowledge_base_member(knowledge_base_id,user_id,member_role) VALUES(1,9,2)");
   c.createStatement().execute("INSERT INTO knowledge_doc(id,knowledge_base_id,title,file_name,format,file_size,status) VALUES(5,1,'证据','证据.pdf',1,10,4),(6,2,'秘密','秘密.txt',5,10,4),(7,1,'等待','等待.pdf',1,10,3)");
   c.createStatement().execute("INSERT INTO knowledge_doc_chunk(id,doc_id,chunk_index,text,start_offset,end_offset,page_number,ocr) VALUES(100,5,0,'水在标准气压下沸点是100度',0,16,2,1),(101,6,0,'秘密证据',0,4,null,0)");
  }
  r.add("spring.datasource.url",()->"jdbc:mysql://localhost:3306/"+DB);
  r.add("spring.datasource.username",()->USER);r.add("spring.datasource.password",()->PASSWORD);
 }
 static Connection connection() throws SQLException{return DriverManager.getConnection("jdbc:mysql://localhost:3306/"+DB,USER,PASSWORD);}
 @BeforeAll void start() throws Exception {token=call("POST","/system/auth/login?id=7",null).path("data").asText();}
 @AfterAll void stop() throws Exception {try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/",USER,PASSWORD)){c.createStatement().execute("DROP DATABASE "+DB);}}
 @BeforeEach void resetExternal(){reset(generation,index);when(index.search(anyString(),any(),any(),any(),anyString(),anyInt())).thenReturn(List.of(new DocIndexHit(100L,0.03,"HYBRID")));when(generation.generate(any())).thenReturn(new RagGenerationPort.GenerationResult(true,"沸点为100度[1]",List.of(1)));}
 @Test void testRag_selectedContextPersistsAndDoesNotSearchThenClearSearches() throws Exception {
  // 准备参数
  long id=call("POST","/knowledge/conversation/create",Map.of("title","所选证据")).path("data").asLong();
  assertEquals("HYBRID",call("GET","/knowledge/conversation/get?id="+id,null).path("data").path("state").path("mode").asText());
  // 调用与断言：选择后及追问只使用选择，清空后恢复检索
  assertTrue(call("POST","/knowledge/rag/ask",Map.of("conversationId",id,"question","沸点？","selectedChunkIds",List.of(100),"modelId",12)).path("data").path("answered").asBoolean());
  assertTrue(call("POST","/knowledge/rag/ask",Map.of("conversationId",id,"question","继续")).path("data").path("answered").asBoolean());
  verifyNoInteractions(index);
  var request=org.mockito.ArgumentCaptor.forClass(RagGenerationPort.GenerationRequest.class);verify(generation,times(2)).generate(request.capture());
  assertEquals(12L,request.getValue().modelId());assertEquals(100L,request.getValue().evidence().getFirst().chunkId());
  var state=call("GET","/knowledge/conversation/get?id="+id,null).path("data").path("state");assertEquals(100,state.path("selectedChunkIds").get(0).asInt());assertEquals("RAG",state.path("mode").asText());
  call("POST","/knowledge/rag/ask",Map.of("conversationId",id,"question","重新检索","selectedChunkIds",List.of()));
  verify(index).search(anyString(),any(),any(),any(),eq("HYBRID"),anyInt());
 }
 @Test void testRag_generationRevocationReplacesAnswerAndDoesNotKeepCitations() throws Exception {
  // 准备参数：生成过程中移除成员，最终答案必须重新授权。
  String owner=token;token=call("POST","/system/auth/login?id=9",null).path("data").asText();
  try{
   long id=call("POST","/knowledge/conversation/create",Map.of("title","撤权")).path("data").asLong();
   when(generation.generate(any())).thenAnswer(i->{try(var c=connection()){c.createStatement().execute("UPDATE knowledge_base_member SET deleted=1 WHERE user_id=9");}return new RagGenerationPort.GenerationResult(true,"沸点为100度[1]",List.of(1));});
   var result=call("POST","/knowledge/rag/ask",Map.of("conversationId",id,"question","沸点","selectedChunkIds",List.of(100))).path("data");
   assertFalse(result.path("answered").asBoolean());assertEquals(0,result.path("citations").size());
   var messages=call("GET","/knowledge/conversation/get?id="+id,null).path("data").path("messages");assertEquals("empty",messages.get(messages.size()-1).path("type").asText());
   assertNotEquals(0,call("GET","/knowledge/search/preview?chunkId=100",null).path("code").asInt());
  }finally{token=owner;try(var c=connection()){c.createStatement().execute("UPDATE knowledge_base_member SET deleted=0 WHERE user_id=9");}}
 }
 @Test void testGuide_twoQuestionsThenHybridAndFailureOrSkipCanSearch() throws Exception {
  // 准备参数：使用明确标识的本地模拟配置，验证真实会话持久化与两轮上限。
  long model;
  try(var c=connection()){
   c.createStatement().execute("INSERT INTO system_model_configuration(name,kind,base_url,api_key,model,timeout_seconds,test_status) VALUES('引导测试','CHAT','mock://local','','mock',5,'SIMULATED')");
   var rows=c.createStatement().executeQuery("SELECT id FROM system_model_configuration WHERE name='引导测试'");rows.next();model=rows.getLong(1);
  }
  try {
   long id=call("POST","/knowledge/conversation/create",Map.of("title","引导检索")).path("data").asLong();
   var first=call("POST","/knowledge/search/guide",Map.of("conversationId",id,"query","查资料","modelId",model)).path("data");
   assertEquals("CLARIFY",first.path("stage").asText());assertEquals(1,first.path("round").asInt());assertEquals(3,first.path("options").size());
   var second=call("POST","/knowledge/search/guide",Map.of("conversationId",id,"query","查资料","answer","看定义")).path("data");
   assertEquals("CLARIFY",second.path("stage").asText());assertEquals(2,second.path("round").asInt());
   var third=call("POST","/knowledge/search/guide",Map.of("conversationId",id,"query","查资料","answer","沸点")).path("data");
   assertEquals("RESULTS",third.path("stage").asText());assertEquals(1,third.path("search").path("total").asInt());
   var state=call("GET","/knowledge/conversation/get?id="+id,null).path("data").path("state");assertEquals("HYBRID",state.path("mode").asText());assertEquals(2,state.path("guideAnswers").size());
   long skip=call("POST","/knowledge/conversation/create",Map.of("title","跳过")).path("data").asLong();
   assertEquals("RESULTS",call("POST","/knowledge/search/guide",Map.of("conversationId",skip,"query","沸点","skip",true)).path("data").path("stage").asText());
   long failure=call("POST","/knowledge/conversation/create",Map.of("title","模型失败")).path("data").asLong();
   var fallback=call("POST","/knowledge/search/guide",Map.of("conversationId",failure,"query","沸点","modelId",99999)).path("data");
   assertEquals("FALLBACK",fallback.path("stage").asText());assertFalse(fallback.path("reason").asText().isBlank());assertEquals(1,fallback.path("search").path("total").asInt());
  } finally {try(var c=connection()){c.createStatement().execute("DELETE FROM system_model_configuration WHERE id="+model);}}
 }

 @Test void testSearch_restoresConversationFiltersAndPreservesMessageSnapshot() throws Exception {
  // 准备参数：会话保存模糊检索和格式过滤，省略本轮选择时恢复自身状态。
  long id=call("POST","/knowledge/conversation/create",Map.of("title","恢复检索")).path("data").asLong();
  call("PUT","/knowledge/conversation/state/update",Map.of("id",id,"state",Map.of("mode","FUZZY","knowledgeBaseIds",List.of(1),"formats",List.of(1))));
  var searched=call("POST","/knowledge/search/search",Map.of("conversationId",id,"query","沸点")).path("data");
  assertEquals(1,searched.path("total").asInt());assertEquals(1,searched.path("indexingCount").asInt());
  verify(index).search(anyString(),any(),eq(List.of(1L)),eq(List.of(5L)),eq("FUZZY"),anyInt());
  // 调用与断言：后续选择不改写历史消息状态。
  call("PUT","/knowledge/conversation/state/update",Map.of("id",id,"state",Map.of("mode","HYBRID","knowledgeBaseIds",List.of(1),"formats",List.of(2))));
  var detail=call("GET","/knowledge/conversation/get?id="+id,null).path("data");
  assertEquals("HYBRID",detail.path("state").path("mode").asText());
  assertEquals("FUZZY",detail.path("messages").get(0).path("content").path("state").path("mode").asText());
 }
 @Test void testPreview_documentContainsAllParsedPositionsButRejectsOtherUser() throws Exception {
  // 准备参数：同份文档第二个分块位于后续原始页。
  try(var c=connection()){c.createStatement().execute("INSERT INTO knowledge_doc_chunk(id,doc_id,chunk_index,text,start_offset,end_offset,page_number) VALUES(102,5,1,'后续页面解析内容',16,24,3)");}
  try(var c=connection()){
   c.createStatement().execute("INSERT INTO knowledge_doc(id,knowledge_base_id,title,file_name,format,file_size,status) VALUES(8,1,'成绩','成绩.xlsx',4,10,4)");
   c.createStatement().execute("INSERT INTO knowledge_doc_chunk(id,doc_id,chunk_index,text,start_offset,end_offset,sheet_name) VALUES(103,8,0,'姓名=张三\\n分数=95',0,11,'成绩')");
  }
  try{
   var document=call("GET","/knowledge/search/document-preview?chunkId=100",null).path("data");
   assertEquals("PDF",document.path("source").path("previewType").asText());
   assertEquals(2,document.path("chunks").size());assertEquals(3,document.path("chunks").get(1).path("pageNumber").asInt());
   assertEquals("后续页面解析内容",document.path("chunks").get(1).path("text").asText());
   var office=call("GET","/knowledge/search/document-preview?chunkId=103",null).path("data");
   assertEquals("OFFICE",office.path("source").path("previewType").asText());assertEquals("成绩",office.path("chunks").get(0).path("sheetName").asText());
   assertTrue(office.path("chunks").get(0).path("text").asText().contains("姓名=张三"));
   assertNotEquals(0,call("GET","/knowledge/search/document-preview?chunkId=101",null).path("code").asInt());
  }finally{try(var c=connection()){c.createStatement().execute("DELETE FROM knowledge_doc_chunk WHERE id IN (102,103)");c.createStatement().execute("DELETE FROM knowledge_doc WHERE id=8");}}
 }

 JsonNode call(String method,String path,Object body) throws Exception{
  var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(token!=null)b.header("Authorization","Bearer "+token);
  b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
  try(var client=HttpClient.newHttpClient()){var response=json.readTree(client.send(b.build(),HttpResponse.BodyHandlers.ofString()).body());if(!path.contains("/preview") && !path.contains("document-preview?chunkId=101"))assertEquals(0,response.path("code").asInt(),response.toString());return response;}
 }
 @SpringBootConfiguration @EnableAutoConfiguration
 @MapperScan({"com.df.knowledge.dal.mysql.doc","com.df.knowledge.dal.mysql.knowledge","com.df.knowledge.dal.mysql.conversation","com.df.knowledge.dal.mysql.model","com.df.knowledge.dal.mysql.permission"})
 @Import({DocGuideServiceImpl.class,com.df.knowledge.config.DefaultDBFieldHandler.class,com.df.knowledge.config.MyBatisPlusConfiguration.class,DocSearchController.class,ConversationController.class,RagController.class,DocSearchServiceImpl.class,ConversationServiceImpl.class,RagServiceImpl.class,KnowledgeBasePermissionServiceImpl.class,ModelRerankService.class,ModelConfigurationServiceImpl.class,OpenAiCompatibleModelClient.class,PermissionServiceImpl.class,StpInterfaceImpl.class,SaTokenConfiguration.class,GlobalExceptionHandler.class,Login.class})
 static class App{
  @Bean DocEmbeddingService embedding(){var e=mock(DocEmbeddingService.class);when(e.embedAll(any())).thenReturn(List.of(new float[512]));return e;}
  @Bean DocChunkIndexService index(){return mock(DocChunkIndexService.class);}
  @Bean RagGenerationPort generation(){return mock(RagGenerationPort.class);}
  @Bean java.util.concurrent.ExecutorService ragStreamExecutor(){return java.util.concurrent.Executors.newCachedThreadPool();}
  @Bean java.util.concurrent.ScheduledExecutorService ragStreamTimeoutExecutor(){return java.util.concurrent.Executors.newSingleThreadScheduledExecutor();}
 }
 @RestController static class Login{@PostMapping("/system/auth/login")CommonResult<String> login(@RequestParam Long id){StpUtil.login(id);return CommonResult.success(StpUtil.getTokenValue());}}
}
