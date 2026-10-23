package com.df.knowledge.dal.dataobject.doc;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文档 DO
 */
@TableName("knowledge_doc")
@Data
@EqualsAndHashCode(callSuper = true)
public class DocDO extends BaseDO {

    /**
     * 知识库编号，关联 {@link KnowledgeBaseDO#getId()}
     */
    private Long knowledgeBaseId;

    /**
     * 文档标题（默认取上传文件名）
     */
    private String title;

    /**
     * 上传时的原始文件名
     */
    private String fileName;

    /**
     * 文档格式，枚举 {@link DocFormatEnum}
     */
    private Integer format;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件编号，关联 {@link com.df.knowledge.dal.dataobject.file.FileDO#getId()}
     */
    private Long fileId;

    /**
     * 文件下载链接（上传接口返回的 url）
     */
    private String fileUrl;

    /**
     * 过渡字段：存量磁盘文件路径，迁移入库后置空，随双读逻辑一并移除
     */
    private String filePath;

    /**
     * 文档状态，枚举 {@link DocStatusEnum}
     */
    private Integer status;

    /** 本次入库提交时间，重试与恢复时更新，服务重启保留 */
    private LocalDateTime ingestSubmitTime;

    /**
     * 失败原因（保留首次失败原因）
     */
    private String failReason;

    /**
     * 删除前状态，枚举 {@link DocStatusEnum}（软删除恢复到删除前状态用）
     */
    private Integer prevStatus;

    /**
     * 删除时间（软删除 30 天保留期起算点）
     */
    private LocalDateTime deleteTime;
}
