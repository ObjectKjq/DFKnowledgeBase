package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 自管 Elasticsearch 分块索引 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DocChunkIndexServiceImpl implements DocChunkIndexService {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Value("${knowledge.elasticsearch.index-name}")
    private String indexName;

    @Override
    public void writeBatch(Long knowledgeBaseId, List<DocChunkDO> chunks, List<float[]> vectors) {
        if (chunks.isEmpty()) {
            return;
        }
        BulkRequest.Builder request = new BulkRequest.Builder().index(indexName).refresh(Refresh.WaitFor);
        for (int i = 0; i < chunks.size(); i++) {
            DocChunkDO chunk = chunks.get(i);
            Map<String, Object> source = convertChunk(knowledgeBaseId, chunk, vectors.get(i));
            request.operations(operation -> operation.index(index -> index.id(chunk.getId().toString()).document(source)));
        }
        try {
            BulkResponse response = elasticsearchClient.bulk(request.build());
            // Bulk 即使 HTTP 成功也可能只有部分条目成功，不能据此把文档标记可用
            if (response.errors()) {
                String reason = response.items().stream().filter(item -> item.error() != null)
                        .map(item -> item.error().reason()).findFirst().orElse("批量写入部分失败");
                throw indexFailure(reason);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.warn("[writeBatch][分块批量写入索引失败]", ex);
            throw indexFailure(ex.getMessage());
        }
    }

    @Override
    public void deleteByDocId(Long docId) {
        try {
            // 删除查询使用可检索快照，先刷新可覆盖上一次中断时尚未刷新的部分写入
            elasticsearchClient.indices().refresh(request -> request.index(indexName));
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(request -> request.index(indexName)
                    .refresh(true).waitForCompletion(true).conflicts(Conflicts.Abort)
                    .query(query -> query.term(term -> term.field("doc_id").value(docId.toString()))));
            Number versionConflicts = response.versionConflicts();
            if (Boolean.TRUE.equals(response.timedOut()) || !response.failures().isEmpty()
                    || (versionConflicts != null && versionConflicts.longValue() > 0)) {
                throw indexFailure("文档索引未完全删除，请重试");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.warn("[deleteByDocId][文档({})索引删除失败]", docId, ex);
            throw indexFailure(ex.getMessage());
        }
    }

    @Override
    public List<Long> searchHybrid(String query, float[] vector, Collection<Long> knowledgeBaseIds,
                                   Collection<Long> docIds, int limit) {
        return search(query, vector, knowledgeBaseIds, docIds, "HYBRID", limit).stream()
                .map(DocIndexHit::chunkId).toList();
    }

    @Override
    public List<DocIndexHit> search(String query, float[] vector, Collection<Long> knowledgeBaseIds,
                                    Collection<Long> docIds, String mode, int limit) {
        if (knowledgeBaseIds.isEmpty() || docIds.isEmpty()) {
            return List.of();
        }
        Query kbFilter = Query.of(q -> q.terms(t -> t.field("kb_id").terms(v -> v.value(
                knowledgeBaseIds.stream().map(id -> FieldValue.of(id.toString())).toList()))));
        Query docFilter = Query.of(q -> q.terms(t -> t.field("doc_id").terms(v -> v.value(
                docIds.stream().map(id -> FieldValue.of(id.toString())).toList()))));
        List<Query> filters = List.of(kbFilter, docFilter);
        List<Float> queryVector = IntStream.range(0, vector.length).mapToObj(i -> vector[i]).toList();
        try {
            // ES Basic 不支持原生 RRF；两路请求独立授权，任一路失败均终止整次检索。
            SearchRequest semantic = SearchRequest.of(s -> s.index(indexName).size(limit)
                    .source(source -> source.fetch(false)).knn(k -> k.field("vector").queryVector(queryVector)
                            .k(limit).numCandidates(Math.min(10000, Math.max(limit, 100))).filter(filters)));
            var semanticHits = checkedSearch(semantic);
            if ("FUZZY".equals(mode)) {
                return semanticHits.stream().map(h -> new DocIndexHit(Long.valueOf(h.id()),
                        h.score() == null ? 0 : h.score(), "SEMANTIC")).toList();
            }
            SearchRequest keyword = SearchRequest.of(s -> s.index(indexName).size(limit)
                    .source(source -> source.fetch(false)).query(q -> q.bool(b -> b.filter(filters)
                            .must(m -> m.match(match -> match.field("text").query(query).analyzer("ik_smart"))))));
            var keywordHits = checkedSearch(keyword);
            Map<Long, Double> scores = new LinkedHashMap<>();
            Map<Long, String> types = new LinkedHashMap<>();
            for (int rank = 0; rank < keywordHits.size(); rank++) {
                Long id = Long.valueOf(keywordHits.get(rank).id());
                scores.merge(id, 1.0 / (61 + rank), Double::sum);
                types.put(id, "KEYWORD");
            }
            for (int rank = 0; rank < semanticHits.size(); rank++) {
                Long id = Long.valueOf(semanticHits.get(rank).id());
                scores.merge(id, 1.0 / (61 + rank), Double::sum);
                types.compute(id, (key, old) -> old == null ? "SEMANTIC" : "HYBRID");
            }
            return scores.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                    .thenComparing(Map.Entry.comparingByKey())).limit(limit)
                    .map(e -> new DocIndexHit(e.getKey(), e.getValue(), types.get(e.getKey()))).toList();
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.warn("[search][分块检索失败]", ex);
            throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_FAILED);
        }
    }

    private List<Hit<Void>> checkedSearch(SearchRequest request) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(request, Void.class);
        if (response.timedOut() || (response.shards() != null && response.shards().failed().intValue() > 0)) {
            throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_FAILED);
        }
        return response.hits().hits();
    }

    /**
     * 字段与 es/create-doc-chunk-index.http 的 mapping 一致，编号按 keyword 写入字符串
     */
    private Map<String, Object> convertChunk(Long knowledgeBaseId, DocChunkDO chunk, float[] vector) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("kb_id", knowledgeBaseId.toString());
        source.put("doc_id", chunk.getDocId().toString());
        source.put("chunk_id", chunk.getId().toString());
        source.put("chunk_index", chunk.getChunkIndex());
        source.put("text", chunk.getText());
        source.put("start_offset", chunk.getStartOffset());
        source.put("end_offset", chunk.getEndOffset());
        source.put("page_number", chunk.getPageNumber());
        source.put("vector", vector);
        return source;
    }

    /**
     * 将外部 ES 错误转换为统一业务异常，失败原因不超过文档表字段上限
     */
    private ServiceException indexFailure(String reason) {
        return new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED,
                StrUtil.subPre(StrUtil.format("文档索引处理失败：{}", StrUtil.blankToDefault(reason, "Elasticsearch 服务异常")), 500));
    }
}
