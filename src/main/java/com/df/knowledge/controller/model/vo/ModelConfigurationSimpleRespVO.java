package com.df.knowledge.controller.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
/** 用户选择命名对话模型使用的最小视图。 */
public record ModelConfigurationSimpleRespVO(@Schema(description = "配置编号") Long id,
        @Schema(description = "配置名称") String name,
        @Schema(description = "是否本地模拟，非真实生成") boolean simulated) { }
