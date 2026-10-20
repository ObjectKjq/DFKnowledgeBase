package com.df.knowledge.enums.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 文档状态枚举
 *
 * 状态迁移唯一依据 PRD §8.2 文档生命周期：排队 → 解析中 → 索引中 → 可用/失败，失败重试回排队，删除为软删除
 */
@Getter
@AllArgsConstructor
public enum DocStatusEnum {

    QUEUED(1, "排队"), // 上传成功，等待入库处理
    PARSING(2, "解析中"), // 文本抽取、切分与分块落库
    INDEXING(3, "索引中"), // 向量化与写入分块索引
    AVAILABLE(4, "可用"), // 入库完成，参与检索与问答
    FAILED(5, "失败"), // 解析或索引异常，可重试（回到排队）
    DELETED(6, "已删除"); // 软删除，30 天内可恢复到删除前状态

    /**
     * 入库进行中的状态集合（排队/解析中/索引中）：重启后由异步执行骨架重新入队
     */
    public static final List<Integer> IN_FLIGHT_STATUS_CODES = List.of(QUEUED.getCode(), PARSING.getCode(), INDEXING.getCode());

    /**
     * 状态
     */
    private final Integer code;

    /**
     * 状态名
     */
    private final String name;

    /**
     * 判断是否为入库进行中的状态（排队/解析中/索引中）
     *
     * @param status 状态
     * @return 是否入库进行中
     */
    public static boolean isInFlight(Integer status) {
        return IN_FLIGHT_STATUS_CODES.contains(status);
    }
}
