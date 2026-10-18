package com.df.knowledge.controller.permission;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.permission.vo.menu.MenuRespVO;
import com.df.knowledge.controller.permission.vo.menu.MenuSaveReqVO;
import com.df.knowledge.service.permission.MenuService;
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
 * 管理后台 - 菜单 Controller
 *
 * 菜单树含目录（1）、菜单（2）与按钮权限点（3）
 */
@Tag(name = "管理后台 - 菜单")
@RestController
@RequestMapping("/system/menu")
@Validated
public class MenuController {

    @Resource
    private MenuService menuService;

    @PostMapping("/create")
    @Operation(summary = "创建菜单")
    @SaCheckPermission("system:menu:create")
    public CommonResult<Long> createMenu(@Valid @RequestBody MenuSaveReqVO reqVO) {
        return success(menuService.createMenu(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改菜单")
    @SaCheckPermission("system:menu:update")
    public CommonResult<Boolean> updateMenu(@Valid @RequestBody MenuSaveReqVO reqVO) {
        menuService.updateMenu(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除菜单", description = "存在子菜单时不允许删除")
    @Parameter(name = "id", description = "菜单编号", required = true, example = "101")
    @SaCheckPermission("system:menu:delete")
    public CommonResult<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取菜单")
    @Parameter(name = "id", description = "菜单编号", required = true, example = "101")
    @SaCheckPermission("system:menu:list")
    public CommonResult<MenuRespVO> getMenu(@RequestParam("id") Long id) {
        return success(BeanUtil.copyProperties(menuService.getMenu(id), MenuRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取全部菜单列表", description = "平铺返回全部菜单（含按钮），按 sort 升序，由前端按 parentId 组装为树")
    @SaCheckPermission("system:menu:list")
    public CommonResult<List<MenuRespVO>> getMenuList() {
        return success(BeanUtil.copyToList(menuService.getMenuList(), MenuRespVO.class));
    }
}
