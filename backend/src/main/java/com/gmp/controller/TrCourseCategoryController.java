package com.gmp.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.entity.TrCourseCategory;
import com.gmp.service.TrCourseCategoryService;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程分类管理
 */
@Api(tags = "课程分类管理")
@RestController
@RequestMapping("/training/course-categories")
@RequiredArgsConstructor
public class TrCourseCategoryController {

    private final TrCourseCategoryService categoryService;

    /** 启用的分类列表（下拉选项） */
    @ApiOperation("查询启用的课程分类列表")
    @GetMapping
    public Result<List<TrCourseCategory>> list() {
        return Result.success(categoryService.listEnabled());
    }

    /** 分页（管理端表格） */
    @ApiOperation("分页查询课程分类")
    @GetMapping("/page")
    public Result<Page<TrCourseCategory>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<TrCourseCategory> p = new Page<>(page, size);
        LambdaQueryWrapper<TrCourseCategory> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(TrCourseCategory::getName, name);
        }
        if (status != null) {
            wrapper.eq(TrCourseCategory::getStatus, status);
        }
        wrapper.orderByAsc(TrCourseCategory::getSort);
        return Result.success(categoryService.page(p, wrapper));
    }

    /** 新增 */
    @ApiOperation("新增课程分类")
    @PostMapping
    public Result<TrCourseCategory> add(@RequestBody TrCourseCategory category) {
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能为空");
        }
        long count = categoryService.count(new LambdaQueryWrapper<TrCourseCategory>()
                .eq(TrCourseCategory::getName, category.getName()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类名称已存在");
        }
        if (category.getSort() == null) { category.setSort(0); }
        if (category.getStatus() == null) { category.setStatus(1); }
        categoryService.save(category);
        return Result.success(category);
    }

    /** 修改 */
    @ApiOperation("修改课程分类")
    @PutMapping("/{id}")
    public Result<TrCourseCategory> update(@PathVariable Long id,
                                            @RequestBody TrCourseCategory category) {
        TrCourseCategory exist = categoryService.getById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程分类不存在");
        }
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能为空");
        }
        long count = categoryService.count(new LambdaQueryWrapper<TrCourseCategory>()
                .eq(TrCourseCategory::getName, category.getName())
                .ne(TrCourseCategory::getId, id));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类名称已存在");
        }
        category.setId(id);
        categoryService.updateById(category);
        return Result.success(category);
    }

    /** 删除（逻辑删除） */
    @ApiOperation("删除课程分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (categoryService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程分类不存在");
        }
        categoryService.removeById(id);
        return Result.success(null);
    }

    /** 启停用 */
    @ApiOperation("启停用课程分类")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 必须是 0 或 1");
        }
        if (categoryService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程分类不存在");
        }
        TrCourseCategory update = new TrCourseCategory();
        update.setId(id);
        update.setStatus(status);
        categoryService.updateById(update);
        return Result.success(null);
    }
}
