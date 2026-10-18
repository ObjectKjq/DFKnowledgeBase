package com.df.knowledge.dal.dataobject.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.enums.auth.LoginResultEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志 DO
 *
 * 只增不改不删，故不继承 BaseDO（无 updater / update_time / deleted）
 */
@TableName("system_login_log")
@Data
public class LoginLogDO {

    /**
     * 日志编号
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户编号，关联 {@link UserDO#getId()}（账号不存在时为空）
     */
    private Long userId;

    /**
     * 用户账号（快照，不随用户删除）
     */
    private String username;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 浏览器 UA
     */
    private String userAgent;

    /**
     * 登录结果，枚举 {@link LoginResultEnum}
     */
    private Integer result;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
