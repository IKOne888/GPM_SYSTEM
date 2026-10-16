package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.CourseAddDTO;
import com.gmp.dto.CourseUpdateDTO;
import com.gmp.dto.CourseVO;
import com.gmp.service.TrCourseService;
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
 * 课程管理Controller
 */
@Api(tags = "课程管理")
@RestController
@RequestMapping("/training/courses")
@RequiredArgsConstructor
@Validated
public class TrCourseController {

    /** 分页大小上限 */
    private static final int MAX_PAGE_SIZE = 100;

    private final TrCourseService courseService;

    @ApiOperation("分页查询课程")
    @GetMapping
    public Result<Page<CourseVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数最小为1") @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return Result.success(courseService.pageCourses(pageNum, pageSize, name, categoryId, status));
    }

    @ApiOperation("查询全部启用课程（下拉选择用）")
    @GetMapping("/list")
    public Result<java.util.List<CourseVO>> list() {
        return Result.success(courseService.listCourses());
    }

    @ApiOperation("新增课程")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CourseAddDTO dto) {
        courseService.addCourse(dto);
        return Result.success();
    }

    @ApiOperation("修改课程")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "课程ID不能为空") Long id,@Valid @RequestBody CourseUpdateDTO dto) {
        // 以路径参数为准，防止篡改
        dto.setId(id);
        courseService.updateCourse(dto);
        return Result.success();
    }

    @ApiOperation("删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "课程ID不能为空") Long id) {
        courseService.removeById(id);
        return Result.success();
    }
}
