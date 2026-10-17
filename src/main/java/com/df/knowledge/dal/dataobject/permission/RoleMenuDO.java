package com.df.knowledge.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色和菜单关联 DO
 */
@TableName("system_role_menu")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleMenuDO extends BaseDO {

    /**
     * 角色编号，关联 {@link RoleDO#getId()}
     */
    private Long roleId;

    /**
     * 菜单编号，关联 {@link MenuDO#getId()}
     */
    private Long menuId;
}
