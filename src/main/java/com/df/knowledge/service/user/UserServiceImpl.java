package com.df.knowledge.service.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.user.vo.user.UserPageReqVO;
import com.df.knowledge.controller.user.vo.user.UserPasswordUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserProfileUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserResetPasswordReqVO;
import com.df.knowledge.controller.user.vo.user.UserSaveReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateStatusReqVO;
import com.df.knowledge.dal.dataobject.permission.UserRoleDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.permission.UserRoleMapper;
import com.df.knowledge.dal.mysql.user.UserMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.knowledge.KnowledgeBaseService;
import com.df.knowledge.service.permission.PermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Service 实现类
 */
@Service
@Validated
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private PermissionService permissionService;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveReqVO reqVO) {
        // 1. 校验账号唯一
        validateUsernameUnique(reqVO.getUsername());
        // 2. 插入用户：密码 BCrypt 加密，状态缺省为开启
        UserDO user = BeanUtil.toBean(reqVO, UserDO.class);
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(CommonStatusEnum.ENABLE.getCode());
        }
        userMapper.insert(user);
        // 3. 创建默认个人知识库「我的知识库」
        knowledgeBaseService.createDefaultKnowledgeBase(user.getId());
        // 4. 分配角色
        assignUserRoles(user.getId(), reqVO.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReqVO reqVO) {
        // 1. 校验用户存在
        validateUserExists(reqVO.getId());
        // 2. 更新资料与状态
        UserDO updateObj = BeanUtil.toBean(reqVO, UserDO.class);
        userMapper.updateById(updateObj);
        // 3. 更新角色分配并失效其权限缓存（权限变更立即生效）
        assignUserRoles(reqVO.getId(), reqVO.getRoleIds());
        permissionService.resetUserCache(reqVO.getId());
        // 4. 停用时强制该用户下线
        if (CommonStatusEnum.DISABLE.getCode().equals(reqVO.getStatus())) {
            StpUtil.kickout(reqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 1. 校验用户存在
        validateUserExists(id);
        // 2. 删除用户及其角色关联
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
        // 3. 失效权限缓存并强制下线
        permissionService.resetUserCache(id);
        StpUtil.kickout(id);
    }

    @Override
    public UserDO getUser(Long id) {
        return validateUserExists(id);
    }

    @Override
    public UserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public PageResult<UserDO> getUserPage(UserPageReqVO reqVO) {
        Page<UserDO> page = userMapper.selectPage(reqVO);
        return PageResult.build(page.getTotal(), page.getRecords());
    }

    @Override
    public List<Long> getUserRoleIdList(Long userId) {
        // 校验用户存在，再返回其角色编号列表
        validateUserExists(userId);
        return userRoleMapper.selectListByUserId(userId).stream()
                .map(UserRoleDO::getRoleId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(UserUpdateStatusReqVO reqVO) {
        // 1. 校验用户存在
        validateUserExists(reqVO.getId());
        // 2. 更新状态
        UserDO updateObj = new UserDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(reqVO.getStatus());
        userMapper.updateById(updateObj);
        // 3. 停用时强制该用户下线
        if (CommonStatusEnum.DISABLE.getCode().equals(reqVO.getStatus())) {
            StpUtil.kickout(reqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(UserResetPasswordReqVO reqVO) {
        // 1. 校验用户存在
        validateUserExists(reqVO.getId());
        // 2. 更新密码（管理员重置无需校验旧密码）
        UserDO updateObj = new UserDO();
        updateObj.setId(reqVO.getId());
        updateObj.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        userMapper.updateById(updateObj);
        // 3. 强制下线，要求以新密码重新登录
        StpUtil.kickout(reqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // 1. 校验用户存在
        validateUserExists(id);
        // 2. 仅允许个人维护昵称、头像与联系方式
        UserDO updateObj = BeanUtil.toBean(reqVO, UserDO.class);
        updateObj.setId(id);
        userMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPassword(Long id, UserPasswordUpdateReqVO reqVO) {
        // 1. 校验用户存在并取出当前密码
        UserDO user = validateUserExists(id);
        // 2. 校验旧密码
        if (!passwordEncoder.matches(reqVO.getOldPassword(), user.getPassword())) {
            throw new ServiceException(ErrorCodeConstants.USER_OLD_PASSWORD_ERROR);
        }
        // 3. 更新密码并强制下线
        UserDO updateObj = new UserDO();
        updateObj.setId(id);
        updateObj.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        userMapper.updateById(updateObj);
        StpUtil.kickout(id);
    }

    @Override
    public void updateUserLoginInfo(Long id, String loginIp, LocalDateTime loginDate) {
        UserDO updateObj = new UserDO();
        updateObj.setId(id);
        updateObj.setLoginIp(loginIp);
        updateObj.setLoginDate(loginDate);
        userMapper.updateById(updateObj);
    }

    /**
     * 校验用户存在
     *
     * @param id 用户编号
     * @return 用户
     */
    private UserDO validateUserExists(Long id) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 校验账号唯一
     *
     * @param username 用户账号
     */
    private void validateUsernameUnique(String username) {
        if (userMapper.selectByUsername(username) != null) {
            throw new ServiceException(ErrorCodeConstants.USER_USERNAME_DUPLICATE,
                    StrUtil.format(ErrorCodeConstants.USER_USERNAME_DUPLICATE.getMsg(), username));
        }
    }

    /**
     * 分配用户角色
     *
     * 入参为 null 时保持现有角色不变，为空列表时清空角色
     *
     * @param userId 用户编号
     * @param roleIds 角色编号列表
     */
    private void assignUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        // 1. 先删除旧的角色关联
        userRoleMapper.deleteByUserId(userId);
        // 2. 批量插入新的角色关联
        if (CollUtil.isNotEmpty(roleIds)) {
            roleIds.forEach(roleId -> userRoleMapper.insert(
                    new UserRoleDO().setUserId(userId).setRoleId(roleId)));
        }
    }
}
