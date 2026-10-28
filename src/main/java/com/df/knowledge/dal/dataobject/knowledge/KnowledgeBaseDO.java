package com.df.knowledge.dal.dataobject.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.enums.knowledge.KnowledgeBaseTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库 DO
 */
@TableName("knowledge_base")
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBaseDO extends BaseDO {

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库类型，枚举 {@link KnowledgeBaseTypeEnum}
     */
    private Integer type;

    /**
     * 所有者用户编号，关联 {@link UserDO#getId()}
     */
    private Long ownerId;

    /**
     * 是否默认知识库（用户创建时自动生成的个人知识库，不可删除）
     */
    private Boolean defaultFlag;

    /**
     * 知识库说明
     */
    private String description;
}
