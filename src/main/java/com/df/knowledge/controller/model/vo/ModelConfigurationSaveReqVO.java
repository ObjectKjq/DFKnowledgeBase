package com.df.knowledge.controller.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/** 保存命名模型；更新时不传密钥表示保留，空字符串表示清除。 */
@Data
public class ModelConfigurationSaveReqVO {
    @Schema(description = "配置编号，创建时为空")
    private Long id;

    @Schema(description = "唯一配置名称")
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "模型用途 CHAT 或 RERANK")
    @NotBlank
    @Pattern(regexp = "CHAT|RERANK")
    private String kind;

    @Schema(description = "OpenAI 兼容服务根地址，例如 http://localhost:1234/v1")
    @NotBlank
    @Size(max = 1000)
    private String baseUrl;

    @Schema(description = "密钥，更新不传则保留，空字符串清除", accessMode = Schema.AccessMode.WRITE_ONLY)
    @Size(max = 2000)
    @lombok.ToString.Exclude
    private String apiKey;

    @Schema(description = "提供方模型名")
    @NotBlank
    @Size(max = 200)
    private String model;

    @Schema(description = "超时秒数，默认 60")
    @Min(1)
    @Max(300)
    private Integer timeoutSeconds = 60;
}
