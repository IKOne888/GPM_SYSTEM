package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.PositionAddDTO;
import com.gmp.dto.PositionMatrixVO;
import com.gmp.dto.PositionUpdateDTO;
import com.gmp.entity.SysPosition;
import com.gmp.service.SysPositionService;
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
 * 岗位管理Controller
 * <p>
 * 路径同时兼容单数(后端历史风格)与复数(前端 RESTful 风格)。
 */
@Api(tags = "岗位管理")
@RestController
@RequestMapping({"/system/position", "/system/positions"})
@RequiredArgsConstructor
@Validated
public class SysPositionController {

    /** 分页大小上限 */
    private static final int MAX_PAGE_SIZE = 100;

    private final SysPositionService positionService;

    @ApiOperation("查询岗位列表")
    @GetMapping("/list")
    public Result<List<SysPosition>> list(@RequestParam(required = false) String keyword) {
        return Result.success(positionService.listPositions(keyword));
    }

    @ApiOperation("分页查询岗位")
    @GetMapping({"", "/page"})
    public Result<Page<SysPosition>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页条数最小为1") @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.success(positionService.pagePositions(page, size, name, status));
    }

    @ApiOperation("新增岗位")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody PositionAddDTO dto) {
        SysPosition position = new SysPosition();
        BeanUtils.copyProperties(dto, position);
        positionService.save(position);
        return Result.success();
    }

    @ApiOperation("修改岗位")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "岗位ID不能为空") Long id,
                               @Valid @RequestBody PositionUpdateDTO dto) {
        // 以路径上的ID为准
        dto.setId(id);
        SysPosition position = new SysPosition();
        BeanUtils.copyProperties(dto, position);
        positionService.updateById(position);
        return Result.success();
    }

    @ApiOperation("删除岗位")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "岗位ID不能为空") Long id) {
        positionService.removeById(id);
        return Result.success();
    }

    @ApiOperation("查询岗位培训矩阵")
    @GetMapping("/{id}/training-matrix")
    public Result<List<PositionMatrixVO>> getTrainingMatrix(@PathVariable @NotNull(message = "岗位ID不能为空") Long id) {
        return Result.success(positionService.getTrainingMatrix(id));
    }

    @ApiOperation("保存岗位培训矩阵")
    @PostMapping("/{id}/training-matrix")
    public Result<Void> saveTrainingMatrix(@PathVariable @NotNull(message = "岗位ID不能为空") Long id,
                                           @RequestBody List<PositionMatrixVO> rows) {
        positionService.saveTrainingMatrix(id, rows);
        return Result.success();
    }
}
