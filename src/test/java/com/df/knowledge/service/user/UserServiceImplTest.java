package com.df.knowledge.service.user;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.user.vo.user.UserPasswordUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserResetPasswordReqVO;
import com.df.knowledge.controller.user.vo.user.UserSaveReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateStatusReqVO;
import com.df.knowledge.dal.dataobject.permission.UserRoleDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.permission.UserRoleMapper;
import com.df.knowledge.dal.mysql.user.UserMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.permission.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testCreateUser_usernameDuplicate() {
        // 准备参数：账号已存在
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername("zhangsan");
        reqVO.setPassword("123456");
        reqVO.setNickname("张三");
        when(userMapper.selectByUsername("zhangsan")).thenReturn(buildUser(1L));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> userService.createUser(reqVO));
        assertEquals(ErrorCodeConstants.USER_USERNAME_DUPLICATE.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testCreateUser_success_encodePasswordAndAssignRoles() {
        // 准备参数
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername("zhangsan");
        reqVO.setPassword("123456");
        reqVO.setNickname("张三");
        reqVO.setRoleIds(List.of(2L));
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        // mock 插入回填自增主键
        doAnswer(invocation -> {
            UserDO user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(UserDO.class));

        // 调用
        Long userId = userService.createUser(reqVO);

        // 断言：密码加密后落库，角色关联已写入
        assertEquals(10L, userId.longValue());
        verify(passwordEncoder).encode("123456");
        verify(userRoleMapper).deleteByUserId(10L);
        verify(userRoleMapper).insert(any(UserRoleDO.class));
    }

    @Test
    void testUpdateUserStatus_disable_kickout() {
        // 准备参数
        UserUpdateStatusReqVO reqVO = new UserUpdateStatusReqVO();
        reqVO.setId(10L);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getCode());
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L));

        // 调用
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            userService.updateUserStatus(reqVO);
            // 断言：停用即强制下线
            stpUtil.verify(() -> StpUtil.kickout(10L));
        }
    }

    @Test
    void testUpdateUserStatus_enable_notKickout() {
        // 准备参数
        UserUpdateStatusReqVO reqVO = new UserUpdateStatusReqVO();
        reqVO.setId(10L);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getCode());
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L));

        // 调用
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            userService.updateUserStatus(reqVO);
            // 断言：开启状态不触发踢下线
            stpUtil.verify(() -> StpUtil.kickout(10L), never());
        }
    }

    @Test
    void testResetUserPassword_kickout() {
        // 准备参数
        UserResetPasswordReqVO reqVO = new UserResetPasswordReqVO();
        reqVO.setId(10L);
        reqVO.setPassword("123456");
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L));
        when(passwordEncoder.encode("123456")).thenReturn("encoded");

        // 调用
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            userService.resetUserPassword(reqVO);
            // 断言：重置密码后强制下线
            stpUtil.verify(() -> StpUtil.kickout(10L));
        }
        verify(userMapper).updateById(any(UserDO.class));
    }

    @Test
    void testUpdateUserPassword_oldPasswordError() {
        // 准备参数：旧密码不匹配
        UserPasswordUpdateReqVO reqVO = new UserPasswordUpdateReqVO();
        reqVO.setOldPassword("wrong");
        reqVO.setNewPassword("654321");
        UserDO user = buildUser(10L);
        user.setPassword("encodedOld");
        when(userMapper.selectById(10L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encodedOld")).thenReturn(false);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> userService.updateUserPassword(10L, reqVO));
        assertEquals(ErrorCodeConstants.USER_OLD_PASSWORD_ERROR.getCode(), exception.getErrorCode().getCode());
        verify(userMapper, never()).updateById(any(UserDO.class));
    }

    @Test
    void testDeleteUser_kickoutAndClearRoles() {
        // 准备参数
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L));

        // 调用
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            userService.deleteUser(10L);
            // 断言：删除用户后强制下线
            stpUtil.verify(() -> StpUtil.kickout(10L));
        }

        // 断言：角色关联清除、权限缓存失效
        verify(userMapper).deleteById(10L);
        verify(userRoleMapper).deleteByUserId(10L);
        verify(permissionService).resetUserCache(10L);
    }

    /**
     * 构造用户 DO
     */
    private static UserDO buildUser(Long id) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setStatus(CommonStatusEnum.ENABLE.getCode());
        return user;
    }
}
