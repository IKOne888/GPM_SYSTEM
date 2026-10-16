package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.demand.DemandHandleDTO;
import com.gmp.dto.demand.DemandSaveDTO;
import com.gmp.dto.demand.DemandVO;
import com.gmp.service.TrTrainingDemandService;
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
 * 培训需求管理 Controller
 * <p>
 * 路径前缀 /training/demands，与前端 views/training/demand/index.vue 的接口契约保持一致。
 * 提供需求的登记、查询、编辑、处理（采纳/拒绝）与删除能力。
 * 权限：由 SaTokenConfig 限制为 SUPER_ADMIN / QA_MANAGER / TRAINING_ADMIN。
 */
@Api(tags = "培训需求管理")
@RestController
@RequestMapping("/training/demands")
@RequiredArgsConstructor
@Validated
public class TrTrainingDemandController {

    /** 分页大小上限，防止恶意大查询拖垮数据库（与系统全局约定一致） */
    private static final int MAX_PAGE_SIZE = 100;

    /** 培训需求 Service */
    private final TrTrainingDemandService demandService;

    /**
     * 分页查询培训需求
     */
    @ApiOperation("分页查询培训需求")
    @GetMapping
    public Result<Page<DemandVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long departmentId) {
        return Result.success(demandService.pageDemands(page, size, keyword, status, departmentId));
    }

    /**
     * 查询需求详情（编辑回显用）
     */
    @ApiOperation("查询培训需求详情")
    @GetMapping("/{id}")
    public Result<DemandVO> detail(@PathVariable @NotNull(message = "需求ID不能为空") Long id) {
        return Result.success(demandService.getDemandDetail(id));
    }

    /**
     * 新增培训需求（登记后状态为「待处理」）
     */
    @ApiOperation("新增培训需求")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DemandSaveDTO dto) {
        demandService.addDemand(dto);
        return Result.success();
    }

    /**
     * 编辑培训需求（仅「待处理」状态可编辑）
     */
    @ApiOperation("编辑培训需求")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "需求ID不能为空") Long id,
                               @Valid @RequestBody DemandSaveDTO dto) {
        // 以路径上的ID为准，防止请求体篡改
        dto.setId(id);
        demandService.updateDemand(dto);
        return Result.success();
    }

    /**
     * 处理培训需求：采纳 / 拒绝
     */
    @ApiOperation("处理培训需求（采纳/拒绝）")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable @NotNull(message = "需求ID不能为空") Long id,
                               @Valid @RequestBody DemandHandleDTO dto) {
        demandService.handleDemand(id, dto);
        return Result.success();
    }

    /**
     * 删除培训需求（仅「待处理」状态可删除）
     */
    @ApiOperation("删除培训需求")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "需求ID不能为空") Long id) {
        demandService.deleteDemand(id);
        return Result.success();
    }
}
