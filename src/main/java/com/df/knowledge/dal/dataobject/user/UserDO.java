package com.df.knowledge.dal.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户 DO
 */
@TableName("system_user")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDO extends BaseDO {

    /**
     * 用户账号，唯一
     */
    private String username;

    /**
     * 密码（BCrypt 加密）
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 账号状态，枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 最后登录 IP
     */
    private String loginIp;

    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;

    /**
     * 备注
     */
    private String remark;
}
