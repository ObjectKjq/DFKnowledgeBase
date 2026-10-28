package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.enums.ErrorCodeConstants;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 内置 bge-small-zh-v15 批量向量化 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DocEmbeddingServiceImpl implements DocEmbeddingService {

    @Resource
    @Lazy
    private EmbeddingModel docEmbeddingModel;

    @Override
    public List<float[]> embedAll(List<String> texts) {
        try {
            return docEmbeddingModel.embedAll(texts.stream().map(TextSegment::from).toList()).content().stream()
                    .map(Embedding::vector).toList();
        } catch (RuntimeException | LinkageError ex) {
            log.warn("[embedAll][内置向量模型批量推理失败]", ex);
            throw new ServiceException(ErrorCodeConstants.DOC_EMBEDDING_FAILED,
                    StrUtil.subPre(StrUtil.format("文档向量化失败：{}", StrUtil.blankToDefault(ex.getMessage(), "模型加载或推理异常")), 500));
        }
    }
}
