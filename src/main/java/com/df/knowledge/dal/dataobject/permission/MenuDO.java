package com.df.knowledge.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.enums.permission.MenuTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单 DO
 */
@TableName("system_menu")
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuDO extends BaseDO {

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 权限标识（目录类型无权限串，留空）
     */
    private String permission;

    /**
     * 菜单类型，枚举 {@link MenuTypeEnum}
     */
    private Integer type;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 父菜单编号（根为 0），关联 {@link MenuDO#getId()}
     */
    private Long parentId;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单状态，枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 子菜单（仅树形结构使用，不落库）
     */
    @TableField(exist = false)
    private List<MenuDO> children;
}
