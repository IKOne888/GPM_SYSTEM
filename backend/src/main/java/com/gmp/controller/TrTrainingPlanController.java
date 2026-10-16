package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.plan.ApprovalDTO;
import com.gmp.dto.plan.PlanListVO;
import com.gmp.dto.plan.PlanSaveDTO;
import com.gmp.dto.plan.PlanVO;
import com.gmp.service.TrTrainingPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 培训计划管理 Controller
 * <p>
 * 路径前缀 /training/plans，与前端 views/training/plan/index.vue 的接口契约保持一致。
 * 提供计划的增删改查、提交审批、审批通过 / 驳回等能力。
 */
@Api(tags = "培训计划管理")
@RestController
@RequestMapping("/training/plans")
@RequiredArgsConstructor
@Validated
public class TrTrainingPlanController {

    /** 分页大小上限，防止恶意大查询拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 培训计划 Service */
    private final TrTrainingPlanService planService;

    /**
     * 分页查询培训计划
     */
    @ApiOperation("分页查询培训计划")
    @GetMapping
    public Result<Page<PlanVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer year) {
        return Result.success(planService.pagePlans(page, size, name, approvalStatus, year));
    }

    /**
     * 查询全部计划（下拉选择用）
     */
    @ApiOperation("查询全部计划（下拉选择用）")
    @GetMapping("/list")
    public Result<List<PlanListVO>> list() {
        return Result.success(planService.listPlans());
    }

    /**
     * 新增培训计划（保存为草稿）
     */
    @ApiOperation("新增培训计划")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody PlanSaveDTO dto) {
        planService.addPlan(dto);
        return Result.success();
    }

    /**
     * 编辑培训计划
     */
    @ApiOperation("编辑培训计划")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "计划ID不能为空") Long id,
                               @Valid @RequestBody PlanSaveDTO dto) {
        // 以路径上的ID为准，防止请求体篡改
        dto.setId(id);
        planService.updatePlan(dto);
        return Result.success();
    }

    /**
     * 提交审批：草稿 / 已驳回 → 待审批
     */
    @ApiOperation("提交审批")
    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable @NotNull(message = "计划ID不能为空") Long id) {
        planService.submitApproval(id);
        return Result.success();
    }

    /**
     * 审批通过：待审批 → 已通过
     */
    @ApiOperation("审批通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable @NotNull(message = "计划ID不能为空") Long id,
                                @RequestBody(required = false) ApprovalDTO dto) {
        planService.approve(id, dto == null ? null : dto.getComment());
        return Result.success();
    }

    /**
     * 驳回：待审批 → 已驳回
     */
    @ApiOperation("驳回计划")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable @NotNull(message = "计划ID不能为空") Long id,
                               @RequestBody(required = false) ApprovalDTO dto) {
        planService.reject(id, dto == null ? null : dto.getComment());
        return Result.success();
    }
}
