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

    private static final int RESULT_LIMIT = 20;

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
        if (StrUtil.isBlank(query)) {
            throw new ServiceException(ErrorCodeConstants.DOC_SEARCH_QUERY_EMPTY);
        }
        String text = query.trim();
        List<Long> accessible = permissionService.getAccessibleKnowledgeBaseIds(userId);
        List<Long> scope = selectedKnowledgeBaseIds == null || selectedKnowledgeBaseIds.isEmpty()
                ? accessible : accessible.stream().filter(selectedKnowledgeBaseIds::contains).toList();
        if (scope.isEmpty()) {
            return List.of();
        }
        List<DocDO> availableDocs = docMapper.selectListByKnowledgeBaseIdsAndStatus(scope,
                DocStatusEnum.AVAILABLE.getCode());
        if (availableDocs.isEmpty()) {
            return List.of();
        }
        List<float[]> vectors = embeddingService.embedAll(List.of(text));
        if (vectors == null || vectors.size() != 1 || vectors.getFirst() == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_EMBEDDING_FAILED);
        }
        List<Long> chunkIds = chunkIndexService.searchHybrid(text, vectors.getFirst(), scope,
                availableDocs.stream().map(DocDO::getId).toList(), RESULT_LIMIT);
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        Map<Long, DocChunkDO> chunks = byId(docChunkMapper.selectBatchIds(chunkIds), DocChunkDO::getId);
        List<Long> docIds = chunkIds.stream().map(chunks::get).filter(chunk -> chunk != null)
                .map(DocChunkDO::getDocId).distinct().toList();
        Map<Long, DocDO> currentDocs = byId(docMapper.selectBatchIds(docIds), DocDO::getId);
        Map<Long, KnowledgeBaseDO> knowledgeBases = byId(knowledgeBaseMapper.selectBatchIds(scope), KnowledgeBaseDO::getId);
        Set<Long> currentAccess = Set.copyOf(permissionService.getAccessibleKnowledgeBaseIds(userId));
        List<DocChunkResult> results = new ArrayList<>();
        for (Long chunkId : chunkIds) {
            DocChunkDO chunk = chunks.get(chunkId);
            DocDO doc = chunk == null ? null : currentDocs.get(chunk.getDocId());
            if (doc != null && DocStatusEnum.AVAILABLE.getCode().equals(doc.getStatus())
                    && scope.contains(doc.getKnowledgeBaseId()) && currentAccess.contains(doc.getKnowledgeBaseId())) {
                KnowledgeBaseDO knowledgeBase = knowledgeBases.get(doc.getKnowledgeBaseId());
                if (knowledgeBase != null) {
                    results.add(toResult(chunk, doc, knowledgeBase));
                }
            }
        }
        return results;
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
                .setText(chunk.getText());
    }

    private static <T> Map<Long, T> byId(Collection<T> values, Function<T, Long> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
