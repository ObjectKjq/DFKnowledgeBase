package com.df.knowledge.controller.user;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.user.vo.user.UserPageReqVO;
import com.df.knowledge.controller.user.vo.user.UserPasswordUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserProfileUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserResetPasswordReqVO;
import com.df.knowledge.controller.user.vo.user.UserRespVO;
import com.df.knowledge.controller.user.vo.user.UserSaveReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateReqVO;
import com.df.knowledge.controller.user.vo.user.UserUpdateStatusReqVO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 管理后台 - 用户 Controller
 *
 * 管理端接口（创建/修改/删除/查询/状态/重置密码）受 RBAC 权限控制；
 * 个人资料与个人改密接口仅要求登录，任意用户可维护自己的信息
 */
@Tag(name = "userController")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/create")
    @Operation(summary = "创建用户", description = "创建用户并分配角色")
    @SaCheckPermission("system:user:create")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        return success(userService.createUser(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户", description = "修改用户资料、状态与角色分配")
    @SaCheckPermission("system:user:update")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserUpdateReqVO reqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "用户编号", required = true, example = "2")
    @SaCheckPermission("system:user:delete")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取用户", description = "返回用户资料与其角色编号列表")
    @Parameter(name = "id", description = "用户编号", required = true, example = "2")
    @SaCheckPermission("system:user:list")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        UserDO user = userService.getUser(id);
        UserRespVO respVO = BeanUtil.copyProperties(user, UserRespVO.class);
        respVO.setRoleIds(userService.getUserRoleIdList(id));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "支持按账号、昵称模糊匹配与状态过滤")
    @SaCheckPermission("system:user:list")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO reqVO) {
        PageResult<UserDO> pageResult = userService.getUserPage(reqVO);
        return success(PageResult.build(pageResult.getTotal(),
                BeanUtil.copyToList(pageResult.getList(), UserRespVO.class)));
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态", description = "停用即强制该用户下线")
    @SaCheckPermission("system:user:update-status")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO);
        return success(true);
    }

    @PutMapping("/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置密码后强制该用户下线")
    @SaCheckPermission("system:user:reset-password")
    public CommonResult<Boolean> resetUserPassword(@Valid @RequestBody UserResetPasswordReqVO reqVO) {
        userService.resetUserPassword(reqVO);
        return success(true);
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料")
    public CommonResult<UserRespVO> getProfile() {
        UserDO user = userService.getUser(StpUtil.getLoginIdAsLong());
        return success(BeanUtil.copyProperties(user, UserRespVO.class));
    }

    @PutMapping("/profile/update")
    @Operation(summary = "修改个人资料")
    public CommonResult<Boolean> updateProfile(@Valid @RequestBody UserProfileUpdateReqVO reqVO) {
        userService.updateUserProfile(StpUtil.getLoginIdAsLong(), reqVO);
        return success(true);
    }

    @PutMapping("/profile/update-password")
    @Operation(summary = "修改个人密码", description = "需校验旧密码，修改成功后强制下线")
    public CommonResult<Boolean> updateProfilePassword(@Valid @RequestBody UserPasswordUpdateReqVO reqVO) {
        userService.updateUserPassword(StpUtil.getLoginIdAsLong(), reqVO);
        return success(true);
    }
}
