package com.df.knowledge.dal.dataobject.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.enums.knowledge.KnowledgeBaseMemberRoleEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库成员 DO
 */
@TableName("knowledge_base_member")
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBaseMemberDO extends BaseDO {

    /**
     * 知识库编号，关联 {@link KnowledgeBaseDO#getId()}
     */
    private Long knowledgeBaseId;

    /**
     * 成员用户编号，关联 {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * 成员角色，枚举 {@link KnowledgeBaseMemberRoleEnum}
     */
    private Integer memberRole;
}
