package com.df.knowledge.service.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 会话当前检索选择，每轮消息保存独立快照，后续修改不改写历史。 */
@Data
public class RetrievalState {
    @Pattern(regexp = "HYBRID|FUZZY|GUIDED|RAG")
    @NotNull
    @Schema(description = "当前模式，新会话默认混合")
    private String mode = "HYBRID";
    @Schema(description = "知识库范围，空数组表示全部有权内容")
    private List<@NotNull @Positive Long> knowledgeBaseIds = List.of();
    @Schema(description = "格式编码过滤")
    private List<Integer> formats = List.of();
    @Schema(description = "上传起点")
    private LocalDateTime uploadedFrom;
    @Schema(description = "上传终点")
    private LocalDateTime uploadedTo;
    @Schema(description = "所选分块，空数组自动检索")
    @Size(max = 100)
    private List<@NotNull @Positive Long> selectedChunkIds = List.of();
    @Schema(description = "所选命名模型编号")
    private Long modelId;
    @Schema(description = "引导原始问题")
    private String guideQuestion;
    @Schema(description = "已回答的澄清轮数，最多两轮")
    @Min(0)
    @Max(2)
    private int guideRound;
    @Schema(description = "澄清回答，支持选项或自由输入")
    private List<String> guideAnswers = List.of();
}
