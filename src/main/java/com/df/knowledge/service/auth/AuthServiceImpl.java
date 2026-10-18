package com.df.knowledge.service.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.auth.vo.auth.AuthLoginReqVO;
import com.df.knowledge.dal.dataobject.auth.LoginLogDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.auth.LoginLogMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.auth.LoginResultEnum;
import com.df.knowledge.service.user.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * 认证 Service 实现类
 */
@Service
@Validated
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserService userService;

    @Resource
    private LoginLogMapper loginLogMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private HttpServletRequest request;

    // 不开启事务：登录失败需照常落库日志，若包在事务里则抛出异常会把失败日志一并回滚
    @Override
    public String login(AuthLoginReqVO reqVO) {
        // 1. 按账号查询用户
        UserDO user = userService.getUserByUsername(reqVO.getUsername());
        // 2. 账号不存在或密码不匹配，记录失败日志后抛出
        if (user == null || !passwordEncoder.matches(reqVO.getPassword(), user.getPassword())) {
            createLoginLog(null, reqVO.getUsername(), LoginResultEnum.BAD_CREDENTIALS);
            throw new ServiceException(ErrorCodeConstants.USER_PASSWORD_ERROR);
        }
        // 3. 账号停用，记录失败日志后抛出
        if (CommonStatusEnum.DISABLE.getCode().equals(user.getStatus())) {
            createLoginLog(user.getId(), reqVO.getUsername(), LoginResultEnum.USER_DISABLED);
            throw new ServiceException(ErrorCodeConstants.USER_IS_DISABLED);
        }
        // 4. 通过 sa-token 颁发令牌（允许同一账号多端并发在线）
        StpUtil.login(user.getId());
        // 5. 记录成功日志与最后登录信息
        createLoginLog(user.getId(), reqVO.getUsername(), LoginResultEnum.SUCCESS);
        userService.updateUserLoginInfo(user.getId(), getClientIp(), LocalDateTime.now());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 记录登录日志
     *
     * @param userId 用户编号（账号不存在时为 null）
     * @param username 用户账号
     * @param result 登录结果
     */
    private void createLoginLog(Long userId, String username, LoginResultEnum result) {
        LoginLogDO loginLog = new LoginLogDO();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginIp(getClientIp());
        loginLog.setUserAgent(request.getHeader("User-Agent"));
        loginLog.setResult(result.getCode());
        loginLogMapper.insert(loginLog);
    }

    /**
     * 获取客户端 IP
     *
     * @return 客户端 IP
     */
    private String getClientIp() {
        return JakartaServletUtil.getClientIP(request);
    }
}
