package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 以 MySQL 可用文档和当前内容权限为准的混合检索 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DocSearchServiceImpl implements DocSearchService {

    private static final int RESULT_LIMIT = 10000;

    @Resource
    private com.df.knowledge.service.model.ModelRerankService modelRerankService;

    @Resource
    private KnowledgeBasePermissionService permissionService;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private DocMapper docMapper;

    @Resource
    private DocChunkMapper docChunkMapper;

    @Resource
    private DocEmbeddingService embeddingService;

    @Resource
    private DocChunkIndexService chunkIndexService;

    @Override
    public List<DocChunkResult> search(Long userId, String query, List<Long> selectedKnowledgeBaseIds) {
        var request = new com.df.knowledge.controller.doc.vo.DocSearchReqVO().setQuery(query)
                .setKnowledgeBaseIds(selectedKnowledgeBaseIds);
        return search(userId, request).getList();
    }

    @Override
    public DocSearchPage search(Long userId, com.df.knowledge.controller.doc.vo.DocSearchReqVO request) {
        if (StrUtil.isBlank(request.getQuery())) throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_QUERY_EMPTY);
        String text = request.getQuery().trim();
        List<Long> accessible = permissionService.getAccessibleKnowledgeBaseIds(userId);
        List<Long> selected = request.getKnowledgeBaseIds();
        List<Long> scope = selected == null || selected.isEmpty() ? accessible
                : accessible.stream().filter(selected::contains).toList();
        var page = new DocSearchPage(); page.setList(List.of()); page.setTotal(0L);
        if (scope.isEmpty()) return page;
        long indexing = 0;
        for (Integer status : DocStatusEnum.IN_FLIGHT_STATUS_CODES) {
            indexing += docMapper.selectListByKnowledgeBaseIdsAndStatus(scope, status).stream()
                    .filter(doc -> matches(doc, request)).count();
        }
        page.setIndexingCount(indexing);
        List<DocDO> availableDocs = docMapper.selectListByKnowledgeBaseIdsAndStatus(scope, 4).stream()
                .filter(doc -> matches(doc, request)).toList();
        if (availableDocs.isEmpty()) return page;
        List<float[]> vectors = embeddingService.embedAll(List.of(text));
        if (vectors == null || vectors.size() != 1 || vectors.getFirst() == null)
            throw new ServiceException(ErrorCodeConstants.DOC_EMBEDDING_FAILED);
        List<DocIndexHit> hits = chunkIndexService.search(text, vectors.getFirst(), scope,
                availableDocs.stream().map(DocDO::getId).toList(), request.getMode(), RESULT_LIMIT);
        if (hits.isEmpty()) return page;
        Map<Long, DocChunkDO> chunks = byId(docChunkMapper.selectBatchIds(hits.stream().map(DocIndexHit::chunkId).toList()), DocChunkDO::getId);
        List<Long> ids = chunks.values().stream().map(DocChunkDO::getDocId).distinct().toList();
        Map<Long, DocDO> currentDocs = byId(docMapper.selectBatchIds(ids), DocDO::getId);
        Map<Long, KnowledgeBaseDO> bases = byId(knowledgeBaseMapper.selectBatchIds(scope), KnowledgeBaseDO::getId);
        Set<Long> currentAccess = Set.copyOf(permissionService.getAccessibleKnowledgeBaseIds(userId));
        List<DocChunkResult> results = new ArrayList<>();
        Set<Long> filteredDocIds = availableDocs.stream().map(DocDO::getId).collect(Collectors.toSet());
        for (DocIndexHit hit : hits) {
            DocChunkDO chunk = chunks.get(hit.chunkId());
            DocDO doc = chunk == null ? null : currentDocs.get(chunk.getDocId());
            if (doc != null && filteredDocIds.contains(doc.getId()) && matches(doc, request)
                    && Integer.valueOf(4).equals(doc.getStatus()) && scope.contains(doc.getKnowledgeBaseId())
                    && currentAccess.contains(doc.getKnowledgeBaseId()) && bases.containsKey(doc.getKnowledgeBaseId())) {
                results.add(toResult(chunk, doc, bases.get(doc.getKnowledgeBaseId()))
                        .setMatchType(hit.matchType()).setScore(hit.score()).setHighlight(highlight(chunk.getText(), text)));
            }
        }
        results = modelRerankService.rerank(text, results);
        page.setTotal((long) results.size()); page.setCandidateLimitReached(hits.size() >= RESULT_LIMIT);
        int from = Math.min(results.size(), (request.getPageNo() - 1) * request.getPageSize());
        page.setList(results.subList(from, Math.min(results.size(), from + request.getPageSize())));
        return page;
    }

    private boolean matches(DocDO doc, com.df.knowledge.controller.doc.vo.DocSearchReqVO request) {
        return (request.getFormats() == null || request.getFormats().isEmpty() || request.getFormats().contains(doc.getFormat()))
                && (request.getUploadedFrom() == null || doc.getCreateTime() != null && !doc.getCreateTime().isBefore(request.getUploadedFrom()))
                && (request.getUploadedTo() == null || doc.getCreateTime() != null && !doc.getCreateTime().isAfter(request.getUploadedTo()));
    }

    private String highlight(String text, String query) {
        String escaped = cn.hutool.http.HtmlUtil.escape(text);
        String needle = cn.hutool.http.HtmlUtil.escape(query);
        return escaped.replace(needle, "<em>" + needle + "</em>");
    }

    @Override
    public boolean hasAvailableDocuments(Long userId, List<Long> selectedKnowledgeBaseIds) {
        List<Long> accessible = permissionService.getAccessibleKnowledgeBaseIds(userId);
        List<Long> scope = selectedKnowledgeBaseIds == null || selectedKnowledgeBaseIds.isEmpty()
                ? accessible : accessible.stream().filter(selectedKnowledgeBaseIds::contains).toList();
        return !docMapper.selectListByKnowledgeBaseIdsAndStatus(scope,
                DocStatusEnum.AVAILABLE.getCode()).isEmpty();
    }

    @Override
    public DocChunkResult getPreview(Long userId, Long chunkId) {
        DocChunkDO chunk = docChunkMapper.selectById(chunkId);
        DocDO doc = chunk == null ? null : docMapper.selectById(chunk.getDocId());
        if (doc == null || !DocStatusEnum.AVAILABLE.getCode().equals(doc.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        try {
            permissionService.checkViewPermission(userId, doc.getKnowledgeBaseId());
        } catch (ServiceException ex) {
            // 分块不存在与无权访问使用相同错误，避免按编号探测其他知识库的内容
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
        if (knowledgeBase == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        return toResult(chunk, doc, knowledgeBase);
    }

    private DocChunkResult toResult(DocChunkDO chunk, DocDO doc, KnowledgeBaseDO knowledgeBase) {
        return new DocChunkResult().setKnowledgeBaseId(doc.getKnowledgeBaseId())
                .setKnowledgeBaseName(knowledgeBase.getName()).setDocId(doc.getId())
                .setDocTitle(doc.getTitle()).setChunkId(chunk.getId())
                .setChunkIndex(chunk.getChunkIndex()).setPageNumber(chunk.getPageNumber())
                .setStartOffset(chunk.getStartOffset()).setEndOffset(chunk.getEndOffset())
                .setText(chunk.getText()).setSheetName(chunk.getSheetName()).setOcr(chunk.getOcr())
                .setFileId(doc.getFileId()).setDownloadUrl(doc.getFileId() == null ? null : "/system/file/download?id=" + doc.getFileId())
                .setFormat(doc.getFormat()).setUploadTime(doc.getCreateTime())
                .setPreviewType(doc.getFileName() != null && doc.getFileName().toLowerCase().endsWith(".pdf") ? "PDF"
                        : doc.getFileName() != null && doc.getFileName().matches("(?i).*\\.(txt|md)$") ? "TEXT" : "OFFICE");
    }

    private static <T> Map<Long, T> byId(Collection<T> values, Function<T, Long> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}

