package com.gmp.controller;

import com.gmp.common.Result;
import com.gmp.dto.role.MenuTreeVO;
import com.gmp.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜单与权限分配Controller
 * <p>
 * 提供菜单树查询、角色菜单绑定等接口。
 * GET /system/menus/tree 允许所有登录用户访问（管理端自身也需要菜单结构来渲染侧边栏），
 * 但菜单绑定接口仅限 SUPER_ADMIN 操作（在 SaTokenConfig 中配置更严格的规则）。
 */
@Api(tags = "菜单与权限分配")
@RestController
@RequiredArgsConstructor
@Validated
public class MenuController {

    /** 角色Service（菜单逻辑收敛在 RoleService 中统一管理） */
    private final RoleService roleService;

    @ApiOperation("查询菜单树（供权限分配弹窗使用）")
    @GetMapping("/system/menus/tree")
    public Result<List<MenuTreeVO>> getMenuTree() {
        return Result.success(roleService.getMenuTree());
    }

    @ApiOperation("查询指定角色已分配的菜单ID列表")
    @GetMapping("/system/roles/{roleId}/menu-ids")
    public Result<List<Long>> listRoleMenuIds(
            @PathVariable @NotNull(message = "角色ID不能为空") Long roleId) {
        return Result.success(roleService.listRoleMenuIds(roleId));
    }

    @ApiOperation("全量替换角色菜单绑定")
    @PostMapping("/system/roles/{roleId}/bind-menus")
    public Result<Void> bindMenus(
            @PathVariable @NotNull(message = "角色ID不能为空") Long roleId,
            @RequestBody List<Long> menuIds) {
        roleService.bindMenus(roleId, menuIds);
        return Result.success();
    }
}
