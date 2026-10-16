package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.role.*;
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
 * 角色管理Controller
 * <p>
 * 提供角色的增删改查、启停用状态切换等RESTful接口。
 * 路径兼容复数形式 /system/roles 以适配前端 RESTful 风格。
 */
@Api(tags = "角色管理")
@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {

    /** 角色Service */
    private final RoleService roleService;

    @ApiOperation("查询全部启用角色（下拉选择用）")
    @GetMapping("/list")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }

    @ApiOperation("分页查询角色列表")
    @GetMapping("/page")
    public Result<Page<RoleVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(roleService.pageRoles(name, code, status, pageNum, pageSize));
    }

    @ApiOperation("新增角色")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success(roleService.addRole(dto));
    }

    @ApiOperation("修改角色")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(dto);
        return Result.success();
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "角色ID不能为空") Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @ApiOperation("切换角色启停用状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable @NotNull(message = "角色ID不能为空") Long id,
                                     @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        roleService.updateStatus(id, status);
        return Result.success();
    }
}
