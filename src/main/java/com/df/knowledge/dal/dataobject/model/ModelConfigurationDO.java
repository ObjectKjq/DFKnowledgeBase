package com.df.knowledge.dal.dataobject.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/** 持久化的命名模型接入配置；密钥仅供内部调用。 */
@Data
@TableName("system_model_configuration")
public class ModelConfigurationDO {
    /** 配置编号。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 唯一显示名称。 */
    private String name;
    /** CHAT 或 RERANK。 */
    private String kind;
    /** OpenAI 兼容服务地址。 */
    private String baseUrl;
    /** 调用密钥，禁止返回前端或写日志。 */
    @lombok.ToString.Exclude
    private String apiKey;
    /** 提供方模型名。 */
    private String model;
    /** 调用超时秒数。 */
    private Integer timeoutSeconds;
    /** 最近测试状态，UNTESTED、SUCCESS 或 FAILED。 */
    private String testStatus;
    /** 最近测试时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastTestTime;
    /** 最近测试的安全错误说明。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String testError;
}
