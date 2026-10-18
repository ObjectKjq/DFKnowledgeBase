package com.df.knowledge.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共字段基类
 *
 * 所有业务 DO 继承本类，统一获得主键、审计字段与逻辑删除
 */
@Data
public abstract class BaseDO implements Serializable {

    /**
     * 主键编号
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建者（登录用户编号）
     */
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者（登录用户编号）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除（逻辑删除）
     */
    @TableLogic
    private Boolean deleted;
}
