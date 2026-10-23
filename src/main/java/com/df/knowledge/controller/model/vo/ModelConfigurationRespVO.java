package com.df.knowledge.controller.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
/** 管理员可读模型配置，不包含密钥。 */
@Data
public class ModelConfigurationRespVO {
    @Schema(description = "配置编号")
    private Long id;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "用途")
    private String kind;

    @Schema(description = "服务地址")
    private String baseUrl;

    @Schema(description = "提供方模型名")
    private String model;

    @Schema(description = "是否已配置密钥")
    private Boolean hasApiKey;

    @Schema(description = "是否本地模拟，true 不代表真实模型连接")
    private Boolean simulated;

    @Schema(description = "超时秒数")
    private Integer timeoutSeconds;

    @Schema(description = "最近测试状态")
    private String testStatus;

    @Schema(description = "最近测试时间")
    private LocalDateTime lastTestTime;

    @Schema(description = "安全错误原因")
    private String testError;
}
