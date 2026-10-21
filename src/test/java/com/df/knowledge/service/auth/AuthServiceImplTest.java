package com.df.knowledge.service.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.auth.vo.auth.AuthLoginReqVO;
import com.df.knowledge.dal.dataobject.auth.LoginLogDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.auth.LoginLogMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.auth.LoginResultEnum;
import com.df.knowledge.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private LoginLogMapper loginLogMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void testLogin_badCredentials_logAndThrow() {
        // 准备参数：账号存在但密码不匹配
        AuthLoginReqVO reqVO = buildLoginReqVO();
        reqVO.setPassword("wrong");
        UserDO user = buildUser(10L, CommonStatusEnum.ENABLE.getCode());
        user.setPassword("encoded");
        when(userService.getUserByUsername("zhangsan")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(reqVO));
        assertEquals(ErrorCodeConstants.USER_PASSWORD_ERROR.getCode(), exception.getErrorCode().getCode());

        // 断言：失败日志落库，结果为账号或密码错
        LoginLogDO loginLog = captureLoginLog();
        assertEquals(LoginResultEnum.BAD_CREDENTIALS.getCode(), loginLog.getResult());
    }

    @Test
    void testLogin_usernameNotFound_logAndThrow() {
        // 准备参数：账号不存在
        AuthLoginReqVO reqVO = buildLoginReqVO();
        when(userService.getUserByUsername("zhangsan")).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(reqVO));
        assertEquals(ErrorCodeConstants.USER_PASSWORD_ERROR.getCode(), exception.getErrorCode().getCode());

        // 断言：失败日志落库
        LoginLogDO loginLog = captureLoginLog();
        assertEquals(LoginResultEnum.BAD_CREDENTIALS.getCode(), loginLog.getResult());
    }

    @Test
    void testLogin_userDisabled_logAndThrow() {
        // 准备参数：密码正确但账号停用
        AuthLoginReqVO reqVO = buildLoginReqVO();
        UserDO user = buildUser(10L, CommonStatusEnum.DISABLE.getCode());
        user.setPassword("encoded");
        when(userService.getUserByUsername("zhangsan")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> authService.login(reqVO));
        assertEquals(ErrorCodeConstants.USER_IS_DISABLED.getCode(), exception.getErrorCode().getCode());

        // 断言：失败日志落库，结果为用户被禁用
        LoginLogDO loginLog = captureLoginLog();
        assertEquals(LoginResultEnum.USER_DISABLED.getCode(), loginLog.getResult());
    }

    @Test
    void testLogin_success_issueTokenAndLog() {
        // 准备参数
        AuthLoginReqVO reqVO = buildLoginReqVO();
        UserDO user = buildUser(10L, CommonStatusEnum.ENABLE.getCode());
        user.setPassword("encoded");
        when(userService.getUserByUsername("zhangsan")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        // 调用
        String token;
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getTokenValue).thenReturn("token-1");
            token = authService.login(reqVO);
            // 断言：以用户编号登录并颁发令牌
            stpUtil.verify(() -> StpUtil.login(10L));
        }

        // 断言
        assertEquals("token-1", token);
        LoginLogDO loginLog = captureLoginLog();
        assertEquals(LoginResultEnum.SUCCESS.getCode(), loginLog.getResult());
        assertEquals(10L, loginLog.getUserId().longValue());
        verify(userService).updateUserLoginInfo(eq(10L), any(), any());
    }

    /**
     * 构造登录请求
     */
    private static AuthLoginReqVO buildLoginReqVO() {
        AuthLoginReqVO reqVO = new AuthLoginReqVO();
        reqVO.setUsername("zhangsan");
        reqVO.setPassword("123456");
        return reqVO;
    }

    /**
     * 构造用户 DO
     */
    private static UserDO buildUser(Long id, Integer status) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    /**
     * 捕获落库的登录日志
     */
    private LoginLogDO captureLoginLog() {
        ArgumentCaptor<LoginLogDO> captor = ArgumentCaptor.forClass(LoginLogDO.class);
        verify(loginLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}
