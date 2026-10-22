package com.df.knowledge.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件 DO（元数据，文件本体见 {@link FileContentDO}）
 */
@TableName("system_file")
@Data
@EqualsAndHashCode(callSuper = true)
public class FileDO extends BaseDO {

    /**
     * 文件名（上传时的原始文件名）
     */
    private String name;

    /**
     * 文件 MIME 类型（Content-Type）
     */
    private String type;

    /**
     * 文件大小（字节）
     */
    private Long size;
}
