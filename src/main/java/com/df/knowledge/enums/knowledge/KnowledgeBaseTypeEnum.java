package com.df.knowledge.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库类型枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeBaseTypeEnum {

    PERSONAL(1, "个人"), // 个人知识库
    SHARED(2, "共享"); // 共享知识库

    /**
     * 类型
     */
    private final Integer code;

    /**
     * 类型名
     */
    private final String name;
}
