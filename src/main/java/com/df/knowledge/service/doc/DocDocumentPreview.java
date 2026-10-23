package com.df.knowledge.service.doc;

import lombok.Data;

import java.util.List;

/** 授权后的整份解析内容及命中位置。 */
@Data
public class DocDocumentPreview {
    /** 原命中分块与原始文件兜底元数据 */
    private DocChunkResult source;
    /** 按文档内顺序返回全部解析分块，保留页码、工作表和原文偏移 */
    private List<DocChunkResult> chunks;
}
