package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.exception.GlobalExceptionHandler;
import com.df.knowledge.dal.dataobject.doc.*;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.doc.*;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.service.doc.*;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import com.df.knowledge.service.model.ModelRerankService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 检索 HTTP 契约旅程；保留真实业务排序、权限重检、过滤与分页。 */
class DocSearchHttpTest {
    @Test void testSearch_defaultPageHasTwentyAndFiltersBeforeRanking() throws Exception {
        // 准备参数
        var service = new DocSearchServiceImpl();
        var permissions = mock(KnowledgeBasePermissionService.class);
        var docs = mock(DocMapper.class); var chunks = mock(DocChunkMapper.class);
        var bases = mock(KnowledgeBaseMapper.class); var embeddings = mock(DocEmbeddingService.class);
        var index = mock(DocChunkIndexService.class); var rerank = mock(ModelRerankService.class);
        ReflectionTestUtils.setField(service,"permissionService",permissions);
        ReflectionTestUtils.setField(service,"docMapper",docs); ReflectionTestUtils.setField(service,"docChunkMapper",chunks);
        ReflectionTestUtils.setField(service,"knowledgeBaseMapper",bases); ReflectionTestUtils.setField(service,"embeddingService",embeddings);
        ReflectionTestUtils.setField(service,"chunkIndexService",index); ReflectionTestUtils.setField(service,"modelRerankService",rerank);
        when(permissions.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of(1L));
        var doc = new DocDO().setKnowledgeBaseId(1L).setTitle("资料.pdf").setFormat(1).setStatus(4);
        doc.setId(5L); doc.setCreateTime(LocalDateTime.of(2026,10,20,0,0));
        when(docs.selectListByKnowledgeBaseIdsAndStatus(any(),eq(4))).thenReturn(List.of(doc));
        when(docs.selectBatchIds(any())).thenReturn(List.of(doc));
        var base = new KnowledgeBaseDO().setName("测试知识库"); base.setId(1L);
        when(bases.selectBatchIds(any())).thenReturn(List.of(base));
        List<DocChunkDO> stored = new ArrayList<>();
        for(long id=100;id<125;id++) { var c=new DocChunkDO().setDocId(5L).setText("知识检索证据").setChunkIndex((int)id-100); c.setId(id); stored.add(c); }
        when(chunks.selectBatchIds(any())).thenReturn(stored);
        when(embeddings.embedAll(any())).thenReturn(List.of(new float[512]));
        when(index.search(anyString(),any(),any(),any(),anyString(),anyInt())).thenReturn(stored.stream()
                .map(c->new DocIndexHit(c.getId(),0.03,"HYBRID")).toList());
        when(rerank.rerank(anyString(),any())).thenAnswer(i->i.getArgument(1));
        var controller=new DocSearchController(); ReflectionTestUtils.setField(controller,"docSearchService",service);
        var mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        try(var auth=mockStatic(StpUtil.class)) { auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            // 调用与断言
            mvc.perform(post("/knowledge/search/search").contentType("application/json").content("{\"query\":\"知识\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(25))
                    .andExpect(jsonPath("$.data.list.length()").value(20))
                    .andExpect(jsonPath("$.data.list[0].highlight").value("<em>知识</em>检索证据"))
                    .andExpect(jsonPath("$.data.list[0].score").value(0.03));
            mvc.perform(post("/knowledge/search/search").contentType("application/json").content("{\"query\":\"知识\",\"pageNo\":2}"))
                    .andExpect(jsonPath("$.data.list.length()").value(5));
            mvc.perform(post("/knowledge/search/search").contentType("application/json").content("{\"query\":\"知识\",\"formats\":[2]}"))
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }
}
