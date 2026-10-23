package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocConstants;
import com.df.knowledge.enums.doc.DocStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 文档批量向量化与检索索引 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DocIndexServiceImpl implements DocIndexService {

    @Resource
    private DocMapper docMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private DocChunkMapper docChunkMapper;

    @Resource
    private DocEmbeddingService docEmbeddingService;

    @Resource
    private DocChunkIndexService docChunkIndexService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void index(Long docId) {
        // 同文档的解析、索引与删除共用行锁，删除完成后旧任务不能把向量写回
        // 共享知识库锁允许同库并行，同时等待删除/恢复的独占生命周期事务结束。
        var knowledgeBase = knowledgeBaseMapper.selectByDocIdForShare(docId);
        DocDO doc = docMapper.selectByIdForUpdate(docId);
        if (doc == null || knowledgeBase == null || knowledgeBase.getDeleteTime() != null
                || !DocStatusEnum.INDEXING.getCode().equals(doc.getStatus())) {
            return;
        }
        List<DocChunkDO> chunks = docChunkMapper.selectListByDocId(docId);
        try {
            // 重启恢复与重试都从权威分块重建，移除上次写入及重新解析前的旧分块编号
            docChunkIndexService.deleteByDocId(docId);
            if (chunks.isEmpty()) {
                throw new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED, "文档索引处理失败：没有可索引的分块");
            }
            for (int start = 0; start < chunks.size(); start += DocConstants.INDEX_BATCH_SIZE) {
                List<DocChunkDO> batch = chunks.subList(start, Math.min(start + DocConstants.INDEX_BATCH_SIZE, chunks.size()));
                List<float[]> vectors = embedBatch(batch);
                docChunkIndexService.writeBatch(doc.getKnowledgeBaseId(), batch, vectors);
            }
            // 状态在行锁释放前发布，避免删除/恢复穿插后旧任务错误标记可用
            if (docMapper.updateStatus(docId, DocStatusEnum.INDEXING.getCode(), DocStatusEnum.AVAILABLE.getCode(), null) == 0) {
                throw new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED, "文档索引处理失败：文档状态已变化");
            }
        } catch (Exception ex) {
            try {
                docChunkIndexService.deleteByDocId(docId);
            } catch (Exception cleanupException) {
                log.error("[index][文档({})失败后的部分索引清理失败]", docId, cleanupException);
            }
            if (ex instanceof ServiceException serviceException) {
                throw serviceException;
            }
            log.warn("[index][文档({})索引处理失败]", docId, ex);
            throw new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED,
                    StrUtil.subPre(StrUtil.format("文档索引处理失败：{}", StrUtil.blankToDefault(ex.getMessage(), "索引服务异常")), 500));
        }
    }

    /**
     * 批量向量化并校验维度及结果顺序，防止错误向量与分块错位后写入索引
     */
    private List<float[]> embedBatch(List<DocChunkDO> batch) {
        try {
            List<float[]> vectors = docEmbeddingService.embedAll(batch.stream().map(DocChunkDO::getText).toList());
            if (vectors == null || vectors.size() != batch.size()) {
                throw new IllegalStateException("向量结果数量与分块数量不一致");
            }
            for (float[] vector : vectors) {
                if (vector == null || vector.length != DocConstants.EMBEDDING_DIMENSION) {
                    throw new IllegalStateException("向量维度必须为 512");
                }
                boolean nonZero = false;
                for (float value : vector) {
                    if (!Float.isFinite(value)) {
                        throw new IllegalStateException("向量包含非有限数值");
                    }
                    nonZero |= value != 0;
                }
                if (!nonZero) {
                    throw new IllegalStateException("余弦相似度不支持全零向量");
                }
            }
            return vectors;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.DOC_EMBEDDING_FAILED,
                    StrUtil.subPre(StrUtil.format("文档向量化失败：{}", StrUtil.blankToDefault(ex.getMessage(), "模型推理异常")), 500));
        }
    }
}
