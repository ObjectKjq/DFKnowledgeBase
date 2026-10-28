package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocChunkDocument;
import com.df.knowledge.dal.elasticsearch.doc.DocChunkRepository;
import com.df.knowledge.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private DocChunkRepository docChunkRepository;

    @Override
    public void writeBatch(Long knowledgeBaseId, List<DocChunkDO> chunks, List<float[]> vectors) {
        if (chunks.isEmpty()) {
            return;
        }
        List<DocChunkDocument> documents = IntStream.range(0, chunks.size())
                .mapToObj(i -> convertChunk(knowledgeBaseId, chunks.get(i), vectors.get(i))).toList();
        try {
            // 等待检索可见后才能发布为可用；逐条写入失败仍由 Spring Data 抛出异常。
            docChunkRepository.saveAll(documents, RefreshPolicy.WAIT_UNTIL);
        } catch (BulkFailureException ex) {
            String reason = ex.getFailedDocuments().values().stream()
                    .map(BulkFailureException.FailureDetails::errorMessage)
                    .filter(StrUtil::isNotBlank).findFirst().orElse("批量写入部分失败");
            throw indexFailure(reason);
        } catch (RuntimeException ex) {
            log.warn("[writeBatch][分块批量写入索引失败]", ex);
            throw indexFailure(ex.getMessage());
        }
    }

    @Override
    public void deleteByDocId(Long docId) {
        try {
            // 删除查询使用可检索快照，先刷新可覆盖上一次中断时尚未刷新的部分写入。
            elasticsearchOperations.indexOps(DocChunkDocument.class).refresh();
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.term(t -> t.field("doc_id").value(docId.toString()))).build();
            DeleteQuery deletion = DeleteQuery.builder(query).withRefresh(true)
                    .withConflicts(ConflictsType.Abort).build();
            // 保留完整响应以检查超时和部分删除失败，不使用仅返回数量的派生删除方法。
            ByQueryResponse response = elasticsearchOperations.delete(deletion, DocChunkDocument.class);
            if (response.getTimedOut() || !response.getFailures().isEmpty()
                    || !response.getSearchFailures().isEmpty() || response.getVersionConflicts() > 0
                    || response.getReasonCancelled() != null) {
                throw indexFailure("文档索引未完全删除，请重试");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
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
            NativeQuery semantic = NativeQuery.builder().withMaxResults(limit)
                    .withSourceFilter(FetchSourceFilter.of(false, null, null))
                    .withKnnSearches(k -> k.field("vector").queryVector(queryVector)
                            .k(limit).numCandidates(Math.min(10000, Math.max(limit, 100))).filter(filters)).build();
            var semanticHits = checkedSearch(semantic);
            if ("FUZZY".equals(mode)) {
                return semanticHits.stream().map(h -> new DocIndexHit(Long.valueOf(h.getId()),
                        h.getScore(), "SEMANTIC")).toList();
            }
            NativeQuery keyword = NativeQuery.builder().withMaxResults(limit)
                    .withSourceFilter(FetchSourceFilter.of(false, null, null))
                    .withQuery(q -> q.bool(b -> b.filter(filters)
                            .must(m -> m.match(match -> match.field("text").query(query).analyzer("ik_smart"))))).build();
            var keywordHits = checkedSearch(keyword);
            Map<Long, Double> scores = new LinkedHashMap<>();
            Map<Long, String> types = new LinkedHashMap<>();
            for (int rank = 0; rank < keywordHits.size(); rank++) {
                Long id = Long.valueOf(keywordHits.get(rank).getId());
                scores.merge(id, 1.0 / (61 + rank), Double::sum);
                types.put(id, "KEYWORD");
            }
            for (int rank = 0; rank < semanticHits.size(); rank++) {
                Long id = Long.valueOf(semanticHits.get(rank).getId());
                scores.merge(id, 1.0 / (61 + rank), Double::sum);
                types.compute(id, (key, old) -> old == null ? "SEMANTIC" : "HYBRID");
            }
            return scores.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                    .thenComparing(Map.Entry.comparingByKey())).limit(limit)
                    .map(e -> new DocIndexHit(e.getKey(), e.getValue(), types.get(e.getKey()))).toList();
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("[search][分块检索失败]", ex);
            throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_FAILED);
        }
    }

    /** 检索超时由传输配置拒绝部分结果，返回后再检查分片失败。 */
    private List<SearchHit<DocChunkDocument>> checkedSearch(NativeQuery query) {
        var response = elasticsearchOperations.search(query, DocChunkDocument.class);
        var shards = response.getSearchShardStatistics();
        if (shards != null && shards.getFailed().intValue() > 0) {
            throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_FAILED);
        }
        return response.getSearchHits();
    }

    /**
     * 字段与 es/create-doc-chunk-index.http 的 mapping 一致，编号按 keyword 写入字符串
     */
    private DocChunkDocument convertChunk(Long knowledgeBaseId, DocChunkDO chunk, float[] vector) {
        return new DocChunkDocument().setKnowledgeBaseId(knowledgeBaseId.toString())
                .setDocId(chunk.getDocId().toString()).setChunkId(chunk.getId().toString())
                .setChunkIndex(chunk.getChunkIndex()).setText(chunk.getText())
                .setStartOffset(chunk.getStartOffset()).setEndOffset(chunk.getEndOffset())
                .setPageNumber(chunk.getPageNumber()).setVector(vector);
    }

    /**
     * 将外部 ES 错误转换为统一业务异常，失败原因不超过文档表字段上限
     */
    private ServiceException indexFailure(String reason) {
        return new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED,
                StrUtil.subPre(StrUtil.format("文档索引处理失败：{}", StrUtil.blankToDefault(reason, "Elasticsearch 服务异常")), 500));
    }
}
