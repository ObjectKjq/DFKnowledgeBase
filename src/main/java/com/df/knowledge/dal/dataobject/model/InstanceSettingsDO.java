package com.df.knowledge.dal.dataobject.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 单实例持久运行设置。 */
@Data
@TableName("system_instance_settings")
public class InstanceSettingsDO {
    /** 单例编号，固定为 1。 */
    @TableId(type = IdType.INPUT)
    private Long id;
    /** MCP 总开关。 */
    private Boolean mcpEnabled;
    /** 每凭证每分钟工具调用上限。 */
    private Integer mcpRateLimitPerMinute;
    /** 全实例入库并发。 */
    private Integer ingestGlobalConcurrency;
    /** 单知识库入库并发。 */
    private Integer ingestKnowledgeBaseConcurrency;
}
