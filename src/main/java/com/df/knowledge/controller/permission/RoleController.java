package com.df.knowledge.controller.permission;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.permission.vo.role.RoleAssignMenuReqVO;
import com.df.knowledge.controller.permission.vo.role.RolePageReqVO;
import com.df.knowledge.controller.permission.vo.role.RoleRespVO;
import com.df.knowledge.controller.permission.vo.role.RoleSaveReqVO;
import com.df.knowledge.controller.permission.vo.role.RoleSimpleRespVO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;
import com.df.knowledge.service.permission.RoleService;
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

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 管理后台 - 角色 Controller
 */
@Tag(name = "roleController")
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;

    @PostMapping("/create")
    @Operation(summary = "创建角色")
    @SaCheckPermission("system:role:create")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleSaveReqVO reqVO) {
        return success(roleService.createRole(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色")
    @SaCheckPermission("system:role:update")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleSaveReqVO reqVO) {
        roleService.updateRole(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色", description = "内置角色不允许删除；存在用户使用该角色时不允许删除")
    @Parameter(name = "id", description = "角色编号", required = true, example = "2")
    @SaCheckPermission("system:role:delete")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取角色")
    @Parameter(name = "id", description = "角色编号", required = true, example = "2")
    @SaCheckPermission("system:role:list")
    public CommonResult<RoleRespVO> getRole(@RequestParam("id") Long id) {
        return success(BeanUtil.copyProperties(roleService.getRole(id), RoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询角色", description = "支持按名称、标识模糊匹配与状态过滤")
    @SaCheckPermission("system:role:list")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(@Valid RolePageReqVO reqVO) {
        PageResult<RoleDO> pageResult = roleService.getRolePage(reqVO);
        return success(PageResult.build(pageResult.getTotal(),
                BeanUtil.copyToList(pageResult.getList(), RoleRespVO.class)));
    }

    @GetMapping("/list")
    @Operation(summary = "获取角色精简列表", description = "仅返回开启状态的角色，供用户分配角色时下拉选择")
    @SaCheckPermission("system:role:list")
    public CommonResult<List<RoleSimpleRespVO>> getSimpleRoleList() {
        return success(BeanUtil.copyToList(roleService.getSimpleRoleList(), RoleSimpleRespVO.class));
    }

    @GetMapping("/get-menu-id-list")
    @Operation(summary = "获取角色拥有的菜单编号列表", description = "供分配菜单时回显已选项")
    @Parameter(name = "roleId", description = "角色编号", required = true, example = "2")
    @SaCheckPermission("system:role:assign-menu")
    public CommonResult<List<Long>> getRoleMenuIdList(@RequestParam("roleId") Long roleId) {
        return success(roleService.getRoleMenuIdList(roleId));
    }

    @PutMapping("/assign-menu")
    @Operation(summary = "分配角色菜单", description = "整体覆盖该角色的菜单权限，变更立即生效")
    @SaCheckPermission("system:role:assign-menu")
    public CommonResult<Boolean> assignRoleMenus(@Valid @RequestBody RoleAssignMenuReqVO reqVO) {
        roleService.assignRoleMenus(reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }
}
