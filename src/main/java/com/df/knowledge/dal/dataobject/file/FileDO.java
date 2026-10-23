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

    /** 上传者用户编号；无法确认归属的历史未关联文件不开放。 */
    private Long uploaderId;

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
