package com.df.knowledge.controller.doc.vo;

import com.df.knowledge.common.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 检索分页响应。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocSearchPageRespVO extends PageResult<DocChunkRespVO> {
    @Schema(description = "当前有权过滤范围内入库中的文档数")
    private long indexingCount;
    @Schema(description = "候选达到一万条窗口，建议缩小过滤范围；total 为当前候选总量")
    private boolean candidateLimitReached;
}
