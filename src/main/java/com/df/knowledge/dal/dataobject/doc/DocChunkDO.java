package com.df.knowledge.dal.dataobject.doc;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档分块 DO
 */
@TableName("knowledge_doc_chunk")
@Data
@EqualsAndHashCode(callSuper = true)
public class DocChunkDO extends BaseDO {

    /** 文档编号，关联 {@link DocDO#getId()} */
    private Long docId;

    /** 块序号，从 0 开始，文档内连续 */
    private Integer chunkIndex;

    /** 分块文本，保留抽取文本中的空白 */
    private String text;

    /** 文档抽取文本中的字符起点（含），按 Java UTF-16 字符计数 */
    private Integer startOffset;

    /** 文档抽取文本中的字符终点（不含），分页文本直接拼接计数 */
    private Integer endOffset;

    /** 页码，从 1 开始；PDF/PPT 按页切分，其余格式为空 */
    private Integer pageNumber;
    /** Excel 工作表名称，其他格式为空 */
    private String sheetName;

    /** 分块是否来自扫描页 OCR */
    private Boolean ocr;
}
