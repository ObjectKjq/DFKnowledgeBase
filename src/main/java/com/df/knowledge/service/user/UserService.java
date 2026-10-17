package com.df.knowledge.service.user;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.user.vo.user.UserPageReqVO;
import com.df.knowledge.controller.user.vo.user.UserPasswordUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserProfileUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserResetPasswordReqVO;
import com.df.knowledge.controller.user.vo.user.UserSaveReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateStatusReqVO;
import com.df.knowledge.dal.dataobject.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Service 接口
 */
public interface UserService {

    /**
     * 创建用户（含角色分配）
     *
     * @param reqVO 创建信息
     * @return 用户编号
     */
    Long createUser(UserSaveReqVO reqVO);

    /**
     * 修改用户资料、状态与角色分配
     *
     * 状态改为停用时强制该用户下线
     *
     * @param reqVO 修改信息（含用户编号）
     */
    void updateUser(UserUpdateReqVO reqVO);

    /**
     * 删除用户
     *
     * 同时清除其角色关联并强制下线
     *
     * @param id 用户编号
     */
    void deleteUser(Long id);

    /**
     * 获取用户
     *
     * @param id 用户编号
     * @return 用户
     */
    UserDO getUser(Long id);

    /**
     * 根据账号获取用户
     *
     * @param username 用户账号
     * @return 用户（不存在时返回 null）
     */
    UserDO getUserByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param reqVO 分页查询条件
     * @return 用户分页
     */
    PageResult<UserDO> getUserPage(UserPageReqVO reqVO);

    /**
     * 获取用户拥有的角色编号列表
     *
     * @param userId 用户编号
     * @return 角色编号列表
     */
    List<Long> getUserRoleIdList(Long userId);

    /**
     * 修改用户状态
     *
     * 停用即强制下线
     *
     * @param reqVO 状态信息（含用户编号）
     */
    void updateUserStatus(UserUpdateStatusReqVO reqVO);

    /**
     * 重置用户密码（管理员操作，无需校验旧密码）
     *
     * 重置后强制下线
     *
     * @param reqVO 重置信息（含用户编号）
     */
    void resetUserPassword(UserResetPasswordReqVO reqVO);

    /**
     * 修改个人资料（当前登录用户）
     *
     * @param id 用户编号
     * @param reqVO 资料信息
     */
    void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO);

    /**
     * 修改个人密码（需校验旧密码），成功后强制下线
     *
     * @param id 用户编号
     * @param reqVO 密码信息
     */
    void updateUserPassword(Long id, UserPasswordUpdateReqVO reqVO);

    /**
     * 更新最后登录信息（登录成功时调用）
     *
     * @param id 用户编号
     * @param loginIp 登录 IP
     * @param loginDate 登录时间
     */
    void updateUserLoginInfo(Long id, String loginIp, LocalDateTime loginDate);
}
