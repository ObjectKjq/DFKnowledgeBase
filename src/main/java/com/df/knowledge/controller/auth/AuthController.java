package com.df.knowledge.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.auth.vo.auth.AuthLoginReqVO;
import com.df.knowledge.controller.auth.vo.auth.AuthLoginRespVO;
import com.df.knowledge.controller.auth.vo.auth.AuthPermissionInfoRespVO;
import com.df.knowledge.dal.dataobject.permission.MenuDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.service.auth.AuthService;
import com.df.knowledge.service.permission.PermissionService;
import com.df.knowledge.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 管理后台 - 认证 Controller
 *
 * 提供登录、登出与登录用户的权限信息（用户信息 + 角色 + 权限串 + 菜单树）
 */
@Tag(name = "authController")
@RestController
@RequestMapping("/system/auth")
@Validated
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private UserService userService;

    @Resource
    private PermissionService permissionService;

    @PostMapping("/login")
    @Operation(summary = "登录", description = "使用账号密码登录，返回访问令牌")
    public CommonResult<AuthLoginRespVO> login(@Valid @RequestBody AuthLoginReqVO reqVO) {
        String token = authService.login(reqVO);
        return success(new AuthLoginRespVO().setUserId(StpUtil.getLoginIdAsLong()).setToken(token));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "退出当前登录会话")
    public CommonResult<Boolean> logout() {
        authService.logout();
        return success(true);
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息", description = "一次返回用户信息、角色标识、权限标识与菜单树")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userService.getUser(userId);
        AuthPermissionInfoRespVO respVO = new AuthPermissionInfoRespVO();
        respVO.setUser(BeanUtil.copyProperties(user, AuthPermissionInfoRespVO.UserVO.class));
        respVO.setRoles(permissionService.getRoleCodeList(userId));
        respVO.setPermissions(permissionService.getPermissionList(userId));
        respVO.setMenus(convertMenuTree(permissionService.getMenuTree(userId)));
        return success(respVO);
    }

    /**
     * 菜单树 DO 转 VO（递归处理 children）
     *
     * @param menus 菜单树
     * @return 菜单树 VO
     */
    private static List<AuthPermissionInfoRespVO.MenuVO> convertMenuTree(List<MenuDO> menus) {
        if (CollUtil.isEmpty(menus)) {
            return Collections.emptyList();
        }
        return menus.stream().map(AuthController::convertMenu).toList();
    }

    /**
     * 单个菜单 DO 转 VO（递归处理 children）
     *
     * @param menu 菜单
     * @return 菜单 VO
     */
    private static AuthPermissionInfoRespVO.MenuVO convertMenu(MenuDO menu) {
        AuthPermissionInfoRespVO.MenuVO menuVO = BeanUtil.copyProperties(menu,
                AuthPermissionInfoRespVO.MenuVO.class);
        menuVO.setChildren(convertMenuTree(menu.getChildren()));
        return menuVO;
    }
}
