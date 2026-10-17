package com.df.knowledge.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户和角色关联 DO
 */
@TableName("system_user_role")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoleDO extends BaseDO {

    /**
     * 用户编号，关联 {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * 角色编号，关联 {@link RoleDO#getId()}
     */
    private Long roleId;
}
