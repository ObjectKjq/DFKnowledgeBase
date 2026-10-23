package com.df.knowledge.service.doc;

import com.df.knowledge.common.PageResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 授权候选的分页检索结果及当前范围内入库提示。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocSearchPage extends PageResult<DocChunkResult> {
    /** 当前授权过滤范围内排队、解析或索引中的文档数量 */
    private long indexingCount;
    /** 候选窗口达到上限时明确提示用户缩小范围 */
    private boolean candidateLimitReached;
}
