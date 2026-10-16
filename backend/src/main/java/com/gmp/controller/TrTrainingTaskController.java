package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.task.TaskAssignDTO;
import com.gmp.dto.task.TaskReviewDTO;
import com.gmp.dto.task.TaskVO;
import com.gmp.service.TrTrainingTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 培训任务管理 Controller
 * <p>
 * 路径前缀 /training/tasks，与前端 views/training/task/index.vue 的接口契约保持一致。
 * 提供任务分页查询、分配、免考确认完成、催办通知，以及「两级审核闭环」的
 * 待办列表、待办统计、主管审核与 QA 复核接口。
 */
@Api(tags = "培训任务管理")
@RestController
@RequestMapping("/training/tasks")
@RequiredArgsConstructor
@Validated
public class TrTrainingTaskController {

    /** 分页大小上限，防止恶意大查询拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 培训任务 Service */
    private final TrTrainingTaskService taskService;

    /**
     * 分页查询培训任务
     */
    @ApiOperation("分页查询培训任务")
    @GetMapping
    public Result<Page<TaskVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeName,
            @RequestParam(required = false) Integer reviewStatus) {
        return Result.success(taskService.pageTasks(page, size, name, status, assigneeName, reviewStatus));
    }

    /**
     * 待我审核列表
     */
    @ApiOperation("待我审核列表")
    @GetMapping("/review/pending")
    public Result<Page<TaskVO>> pendingReviews(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String stage) {
        return Result.success(taskService.pendingReviews(page, size, stage));
    }

    /**
     * 待审核数量统计（页签角标）
     */
    @ApiOperation("待审核数量统计")
    @GetMapping("/review/stats")
    public Result<Map<String, Long>> reviewStats() {
        return Result.success(taskService.reviewStats());
    }

    /**
     * 一级审核（部门主管审核）
     */
    @ApiOperation("主管审核")
    @PostMapping("/{id}/leader-review")
    public Result<Void> leaderReview(@PathVariable @NotNull(message = "任务ID不能为空") Long id,
                                     @Valid @RequestBody TaskReviewDTO dto) {
        taskService.leaderReview(id, dto);
        return Result.success();
    }

    /**
     * 二级复核（QA 复核）
     */
    @ApiOperation("QA复核")
    @PostMapping("/{id}/qa-review")
    public Result<Void> qaReview(@PathVariable @NotNull(message = "任务ID不能为空") Long id,
                                 @Valid @RequestBody TaskReviewDTO dto) {
        taskService.qaReview(id, dto);
        return Result.success();
    }

    /**
     * 分配培训任务（支持批量分配）
     */
    @ApiOperation("分配培训任务")
    @PostMapping("/assign")
    public Result<Void> assign(@Valid @RequestBody TaskAssignDTO dto) {
        taskService.assign(dto);
        return Result.success();
    }

    /**
     * 免考确认完成（仅培训管理员/系统管理员，必须填写确认理由）
     */
    @ApiOperation("免考确认完成")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable @NotNull(message = "任务ID不能为空") Long id,
                                 @RequestParam @NotBlank(message = "确认理由不能为空") String remark) {
        taskService.complete(id, remark);
        return Result.success();
    }

    /**
     * 催办通知
     */
    @ApiOperation("催办通知")
    @PostMapping("/{id}/notify")
    public Result<Void> notifyTask(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        taskService.notifyTask(id);
        return Result.success();
    }
}
