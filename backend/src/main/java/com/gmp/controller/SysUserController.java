package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.BatchDeleteDTO;
import com.gmp.dto.UserAddDTO;
import com.gmp.dto.UserListVO;
import com.gmp.dto.UserStatusDTO;
import com.gmp.dto.UserUpdateDTO;
import com.gmp.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 用户管理Controller
 * 统一使用复数 RESTful 路径 /system/users，与前端 views/system/user/index.vue 对齐，
 * 避免出现"请求资源不存在"的 404 问题。
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@Validated
public class SysUserController {

    /** 分页大小上限，防止恶意大查询拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    private final SysUserService userService;

    /** 分页查询用户：前端传 page/size/username/departmentId/status */
    @ApiOperation("分页查询用户")
    @GetMapping
    public Result<Page<UserListVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.pageUsers(page, size, username, departmentId, status));
    }

    /** 查询启用用户下拉列表（任务分配、执行人选择用，脱敏返回） */
    @ApiOperation("查询用户下拉列表")
    @GetMapping("/list")
    public Result<java.util.List<com.gmp.dto.UserSimpleVO>> list() {
        return Result.success(userService.listUsers());
    }

    /** 新增用户 */
    @ApiOperation("新增用户")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserAddDTO dto) {
        userService.addUser(dto);
        return Result.success();
    }

    /** 修改用户：以路径 ID 为准 */
    @ApiOperation("修改用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "用户ID不能为空") Long id,
                               @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userService.updateUser(dto);
        return Result.success();
    }

    /** 删除用户 */
    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /** 批量删除用户 */
    @ApiOperation("批量删除用户")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        userService.batchDelete(dto.getIds());
        return Result.success();
    }

    /** 修改用户状态（启用/禁用） */
    @ApiOperation("修改用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable @NotNull(message = "用户ID不能为空") Long id,
                                     @Valid @RequestBody UserStatusDTO dto) {
        userService.changeStatus(id, dto.getStatus());
        return Result.success();
    }

    /** 重置密码为系统默认密码 */
    @ApiOperation("重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        userService.resetPassword(id, null);
        return Result.success();
    }
}