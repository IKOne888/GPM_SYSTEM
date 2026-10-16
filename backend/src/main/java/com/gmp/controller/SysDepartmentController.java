package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.DepartmentAddDTO;
import com.gmp.dto.DepartmentUpdateDTO;
import com.gmp.entity.SysDepartment;
import com.gmp.service.SysDepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 部门管理Controller
 * <p>
 * 路径同时兼容单数(后端历史风格)与复数(前端 RESTful 风格)。
 */
@Api(tags = "部门管理")
@RestController
@RequestMapping({"/system/department", "/system/departments"})
@RequiredArgsConstructor
@Validated
public class SysDepartmentController {

    /** 分页大小上限 */
    private static final int MAX_PAGE_SIZE = 100;

    private final SysDepartmentService departmentService;

    @ApiOperation("查询部门列表")
    @GetMapping("/list")
    public Result<List<SysDepartment>> list(@RequestParam(required = false) String keyword) {
        return Result.success(departmentService.listDepartments(keyword));
    }

    @ApiOperation("查询部门树")
    @GetMapping("/tree")
    public Result<List<SysDepartment>> tree() {
        return Result.success(departmentService.listTree());
    }

    @ApiOperation("分页查询部门")
    @GetMapping("/page")
    public Result<Page<SysDepartment>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数最小为1") @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(departmentService.pageDepartments(pageNum, pageSize, keyword));
    }

    @ApiOperation("新增部门")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DepartmentAddDTO dto) {
        SysDepartment department = new SysDepartment();
        BeanUtils.copyProperties(dto, department);
        departmentService.save(department);
        return Result.success();
    }

    @ApiOperation("修改部门")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "部门ID不能为空") Long id,
                               @Valid @RequestBody DepartmentUpdateDTO dto) {
        // 以路径上的ID为准
        dto.setId(id);
        SysDepartment department = new SysDepartment();
        BeanUtils.copyProperties(dto, department);
        departmentService.updateById(department);
        return Result.success();
    }

    @ApiOperation("删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "部门ID不能为空") Long id) {
        departmentService.removeById(id);
        return Result.success();
    }
}
