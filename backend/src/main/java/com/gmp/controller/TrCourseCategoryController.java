package com.gmp.controller;

import com.gmp.common.Result;
import com.gmp.entity.TrCourseCategory;
import com.gmp.service.TrCourseCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程分类Controller
 */
@Api(tags = "课程分类管理")
@RestController
@RequestMapping("/training/course-categories")
@RequiredArgsConstructor
public class TrCourseCategoryController {

    private final TrCourseCategoryService categoryService;

    @ApiOperation("查询启用的课程分类列表")
    @GetMapping
    public Result<List<TrCourseCategory>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
