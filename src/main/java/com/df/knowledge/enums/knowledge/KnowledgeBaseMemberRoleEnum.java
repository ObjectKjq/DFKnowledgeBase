package com.df.knowledge.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库成员角色枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeBaseMemberRoleEnum {

    EDITOR(1, "编辑者"), // 编辑者：可查看、检索、问答与内容写入
    VIEWER(2, "查看者"); // 查看者：仅可查看、检索、问答

    /**
     * 角色
     */
    private final Integer code;

    /**
     * 角色名
     */
    private final String name;
}
