package com.df.knowledge.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件内容 DO（与 {@link FileDO} 1:1，字节本体存数据库，ADR-0009）
 */
@TableName("system_file_content")
@Data
@EqualsAndHashCode(callSuper = true)
public class FileContentDO extends BaseDO {

    /**
     * 文件编号，关联 {@link FileDO#getId()}
     */
    private Long fileId;

    /**
     * 文件内容（二进制）
     */
    private byte[] content;
}
