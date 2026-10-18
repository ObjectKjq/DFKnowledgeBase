package com.df.knowledge.service.auth;

import com.df.knowledge.controller.auth.vo.auth.AuthLoginReqVO;

/**
 * 认证 Service 接口
 */
public interface AuthService {

    /**
     * 使用账号密码登录
     *
     * 登录成功后通过 sa-token 颁发 token；失败（密码错误 / 账号停用）记录登录日志并抛业务异常
     *
     * @param reqVO 登录信息
     * @return 访问令牌
     */
    String login(AuthLoginReqVO reqVO);

    /**
     * 登出当前登录用户
     */
    void logout();
}
