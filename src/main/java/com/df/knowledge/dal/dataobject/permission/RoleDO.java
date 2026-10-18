package com.df.knowledge.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色 DO
 */
@TableName("system_role")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleDO extends BaseDO {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色权限标识，唯一
     */
    private String code;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 角色状态，枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 角色类型，枚举 {@link RoleTypeEnum}
     */
    private Integer type;

    /**
     * 备注
     */
    private String remark;
}
